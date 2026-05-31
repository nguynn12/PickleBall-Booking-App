const axios = require('axios');
const crypto = require('crypto');
const admin = require('firebase-admin');

const PAYOS_CLIENT_ID = process.env.PAYOS_CLIENT_ID;
const PAYOS_API_KEY = process.env.PAYOS_API_KEY;
const PAYOS_CHECKSUM = process.env.PAYOS_CHECKSUM_KEY;

exports.createPaymentLink = async (req, res) => {
    res.set('Access-Control-Allow-Origin', '*');
    if (req.method === 'OPTIONS') {
        res.set('Access-Control-Allow-Methods', 'POST');
        res.set('Access-Control-Allow-Headers', 'Content-Type');
        return res.status(204).send('');
    }

    const { bookingId, amount, customerName, courtName } = req.body || {};
    if (!bookingId || !amount) {
        return res.status(400).json({ success: false, error: 'Missing required fields' });
    }

    const bookingRef = admin.firestore().collection('Bookings').doc(bookingId);
    const bookingSnap = await bookingRef.get();
    const booking = bookingSnap.exists ? bookingSnap.data() : null;

    if (booking?.paymentStatus === 'paid') {
        return res.json({ success: true, alreadyPaid: true });
    }

    const reusable =
        booking?.paymentStatus === 'pending'
        && booking?.payosOrderCode
        && booking?.payosQrCode
        && booking?.paymentExpiredAt
        && booking.paymentExpiredAt > Date.now();

    if (reusable) {
        return res.json({
            success: true,
            qrCode: booking.payosQrCode,
            checkoutUrl: booking.payosCheckoutUrl || '',
            orderCode: booking.payosOrderCode,
            depositAmount: booking.depositAmount || Math.round(amount * 0.30),
            reused: true
        });
    }

    const depositAmount = Math.round(amount * 0.30);
    const orderCode = parseInt(Date.now().toString().slice(-9), 10);
    const description = `Coc san ${bookingId.slice(-6)}`;
    const cancelUrl = 'https://picklebook.app/payment/cancel';
    const returnUrl = 'https://picklebook.app/payment/success';

    const dataStr = `amount=${depositAmount}&cancelUrl=${cancelUrl}&description=${description}&orderCode=${orderCode}&returnUrl=${returnUrl}`;
    const signature = crypto
        .createHmac('sha256', PAYOS_CHECKSUM)
        .update(dataStr)
        .digest('hex');

    const payload = {
        orderCode,
        amount: depositAmount,
        description,
        cancelUrl,
        returnUrl,
        signature,
        items: [{
            name: `Dat san ${courtName || 'Pickleball'}`,
            quantity: 1,
            price: depositAmount
        }],
        buyerName: customerName || 'Khach hang',
        expiredAt: Math.floor(Date.now() / 1000) + 900
    };

    try {
        const response = await axios.post(
            'https://api-merchant.payos.vn/v2/payment-requests',
            payload,
            {
                headers: {
                    'x-client-id': PAYOS_CLIENT_ID,
                    'x-api-key': PAYOS_API_KEY,
                    'Content-Type': 'application/json'
                }
            }
        );

        const { checkoutUrl, qrCode, paymentLinkId } = response.data.data;

        await bookingRef.update({
            payosOrderCode: orderCode,
            payosPaymentLinkId: paymentLinkId,
            payosQrCode: qrCode,
            payosCheckoutUrl: checkoutUrl,
            depositAmount,
            paymentStatus: 'pending',
            paymentExpiredAt: Date.now() + 900_000
        });

        return res.json({ success: true, qrCode, checkoutUrl, orderCode, depositAmount });
    } catch (error) {
        console.error('PayOS error:', error.response?.data || error.message);
        return res.status(500).json({ success: false, error: 'Error creating payment QR' });
    }
};
