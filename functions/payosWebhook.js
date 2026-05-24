const crypto = require('crypto');
const admin  = require('firebase-admin');

exports.payosWebhook = async (req, res) => {
    const PAYOS_CHECKSUM = process.env.PAYOS_CHECKSUM_KEY;

    // 1. Verify chữ ký từ PayOS
    const { code, data, signature } = req.body;

    const dataStr = Object.keys(data)
        .sort()
        .map(k => `${k}=${data[k]}`)
        .join('&');

    const expectedSig = crypto
        .createHmac('sha256', PAYOS_CHECKSUM)
        .update(dataStr)
        .digest('hex');

    if (expectedSig !== signature) {
        console.error('Invalid webhook signature');
        return res.status(400).json({ error: 'Invalid signature' });
    }

    // 2. Chỉ xử lý khi thanh toán thành công (code = "00")
    if (code !== '00') {
        return res.json({ success: true, note: 'Non-success event, ignored' });
    }

    const orderCode = data.orderCode;

    // 3. Tìm booking theo payosOrderCode
    const snap = await admin.firestore()
        .collection('Bookings')
        .where('payosOrderCode', '==', orderCode)
        .limit(1)
        .get();

    if (snap.empty) {
        console.error('Booking not found for orderCode:', orderCode);
        return res.status(404).json({ error: 'Booking not found' });
    }

    const bookingDoc = snap.docs[0];
    const booking    = bookingDoc.data();

    // 4. Kiểm tra conflict (slot bị người khác đặt confirmed trong lúc chờ)
    const conflictSnap = await admin.firestore()
        .collection('Bookings')
        .where('courtId',   '==', booking.courtId)
        .where('date',      '==', booking.date)
        .where('status',    '==', 'confirmed')
        .where('subCourtId','==', booking.subCourtId || '')
        .get();

    // Loại bỏ chính booking hiện tại khỏi conflict check
    const realConflicts = conflictSnap.docs.filter(d => d.id !== bookingDoc.id);

    if (realConflicts.length > 0) {
        // Conflict → hủy, cần hoàn tiền
        await bookingDoc.ref.update({
            status:       'cancelled_conflict',
            refundStatus: 'refund_pending',
            refundAmount:  booking.depositAmount || 0
        });
        await sendNotification(
            booking.userId,
            '⚠️ Lịch đặt bị hủy do trùng slot',
            `Rất tiếc, slot bạn đặt tại sân ${booking.courtName} ngày ${booking.date} đã bị người khác đặt trước. Tiền cọc sẽ được hoàn lại.`
        );
        return res.json({ success: true, note: 'Conflict — cancelled and refund queued' });
    }

    // 5. Xác nhận booking thành công
    await bookingDoc.ref.update({
        status:             'confirmed',
        paymentStatus:      'paid',
        paidAt:             admin.firestore.FieldValue.serverTimestamp(),
        payosTransactionId: data.id || ''
    });

    // 6. Gửi thông báo
    await sendNotification(
        booking.userId,
        '✅ Đặt sân thành công!',
        `Bạn đã cọc thành công cho sân ${booking.courtName} vào ngày ${booking.date} lúc ${booking.startTime}.`
    );
    if (booking.ownerId) {
        await sendNotification(
            booking.ownerId,
            '🏓 Có lịch đặt mới!',
            `Khách đã đặt sân ${booking.courtName} ngày ${booking.date} lúc ${booking.startTime}.`
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
        isRead:    false,
        createdAt: admin.firestore.FieldValue.serverTimestamp()
    });
}
