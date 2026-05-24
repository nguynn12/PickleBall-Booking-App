const admin = require('firebase-admin');

/** Cron job — hủy booking chưa thanh toán sau 15 phút */
exports.cancelExpiredBookings = async (context) => {
    const now = Date.now();

    const snap = await admin.firestore()
        .collection('Bookings')
        .where('status', '==', 'awaiting_payment')
        .where('paymentExpiredAt', '<', now)
        .get();

    if (snap.empty) {
        console.log('No expired bookings found');
        return null;
    }

    const batch = admin.firestore().batch();
    snap.docs.forEach(doc => {
        batch.update(doc.ref, {
            status:        'cancelled_timeout',
            paymentStatus: 'expired',
            cancelledBy:   'system',
            cancelledAt:   now
        });
    });

    await batch.commit();
    console.log(`Cancelled ${snap.size} expired bookings`);
    return null;
};

/** Cron job — đánh dấu completed cho booking đã qua giờ chơi */
exports.markCompletedBookings = async (context) => {
    const now    = new Date();
    const today  = `${String(now.getDate()).padStart(2,'0')}/${String(now.getMonth()+1).padStart(2,'0')}/${now.getFullYear()}`;
    const nowHHMM = `${String(now.getHours()).padStart(2,'0')}:${String(now.getMinutes()).padStart(2,'0')}`;

    const snap = await admin.firestore()
        .collection('Bookings')
        .where('status', '==', 'confirmed')
        .where('date',   '==', today)
        .get();

    const batch = admin.firestore().batch();
    let count = 0;
    snap.docs.forEach(doc => {
        const endTime = doc.data().endTime || '';
        if (endTime && endTime <= nowHHMM) {
            batch.update(doc.ref, { status: 'completed' });
            count++;
        }
    });

    if (count > 0) await batch.commit();
    console.log(`Marked ${count} bookings as completed`);
    return null;
};
