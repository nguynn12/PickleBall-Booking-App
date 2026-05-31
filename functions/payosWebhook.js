const crypto = require('crypto');
const admin = require('firebase-admin');

exports.payosWebhook = async (req, res) => {
    res.set('Access-Control-Allow-Origin', '*');

    if (req.method === 'OPTIONS') {
        res.set('Access-Control-Allow-Methods', 'GET, POST');
        res.set('Access-Control-Allow-Headers', 'Content-Type');
        return res.status(204).send('');
    }

    // PayOS dashboard may ping this URL before saving it.
    if (req.method === 'GET') {
        return res.status(200).json({ success: true, message: 'PayOS webhook is active' });
    }

    const PAYOS_CHECKSUM = process.env.PAYOS_CHECKSUM_KEY;
    const { code, data, signature } = req.body || {};

    // Treat an empty dashboard validation POST as a health check.
    if (!data || !signature) {
        return res.status(200).json({ success: true, message: 'Webhook endpoint is ready' });
    }

    const dataStr = Object.keys(data)
        .sort()
        .map((key) => `${key}=${data[key]}`)
        .join('&');

    const expectedSig = crypto
        .createHmac('sha256', PAYOS_CHECKSUM)
        .update(dataStr)
        .digest('hex');

    if (expectedSig !== signature) {
        console.error('Invalid webhook signature');
        return res.status(400).json({ error: 'Invalid signature' });
    }

    if (code !== '00') {
        return res.json({ success: true, note: 'Non-success event ignored' });
    }

    const rawOrderCode = data.orderCode;
    const numericOrderCode = Number(rawOrderCode);
    let snap = await admin.firestore()
        .collection('Bookings')
        .where('payosOrderCode', '==', Number.isFinite(numericOrderCode) ? numericOrderCode : rawOrderCode)
        .limit(1)
        .get();

    if (snap.empty) {
        snap = await admin.firestore()
            .collection('Bookings')
            .where('payosOrderCode', '==', String(rawOrderCode))
            .limit(1)
            .get();
    }

    if (snap.empty) {
        console.error('Booking not found for orderCode:', rawOrderCode);
        return res.status(200).json({ success: true, note: 'Booking not found, event acknowledged' });
    }

    const bookingDoc = snap.docs[0];
    const booking = bookingDoc.data();

    const conflictSnap = await admin.firestore()
        .collection('Bookings')
        .where('courtId', '==', booking.courtId)
        .where('date', '==', booking.date)
        .where('status', '==', 'confirmed')
        .where('subCourtId', '==', booking.subCourtId || '')
        .get();

    const realConflicts = conflictSnap.docs.filter((doc) => doc.id !== bookingDoc.id);

    if (realConflicts.length > 0) {
        await bookingDoc.ref.update({
            status: 'cancelled_conflict',
            refundStatus: 'refund_pending',
            refundAmount: booking.depositAmount || 0,
            paymentStatus: 'paid',
            paidAt: admin.firestore.FieldValue.serverTimestamp(),
            payosTransactionId: data.id || ''
        });
        await sendNotification(
            booking.userId,
            'Lich dat bi huy do trung slot',
            `Slot tai san ${booking.courtName} ngay ${booking.date} da bi dat truoc. Tien coc se duoc hoan lai.`
        );
        return res.json({ success: true, note: 'Conflict cancelled and refund queued' });
    }

    await bookingDoc.ref.update({
        status: 'confirmed',
        paymentStatus: 'paid',
        paidAt: admin.firestore.FieldValue.serverTimestamp(),
        payosTransactionId: data.id || ''
    });

    await sendNotification(
        booking.userId,
        'Dat san thanh cong!',
        `Ban da coc thanh cong cho san ${booking.courtName} vao ngay ${booking.date} luc ${booking.startTime}.`
    );
    if (booking.ownerId) {
        await sendNotification(
            booking.ownerId,
            'Co lich dat moi!',
            `Khach da dat san ${booking.courtName} ngay ${booking.date} luc ${booking.startTime}.`
        );
    }

    return res.json({ success: true });
};

async function sendNotification(userId, title, message) {
    if (!userId) return;
    await admin.firestore().collection('Notifications').add({
        userId,
        title,
        message,
        isRead: false,
        createdAt: admin.firestore.FieldValue.serverTimestamp()
    });
}
