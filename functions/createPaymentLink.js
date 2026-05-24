const axios  = require('axios');
const crypto = require('crypto');

const PAYOS_CLIENT_ID = process.env.PAYOS_CLIENT_ID;
const PAYOS_API_KEY   = process.env.PAYOS_API_KEY;
const PAYOS_CHECKSUM  = process.env.PAYOS_CHECKSUM_KEY;

exports.createPaymentLink = async (req, res) => {
    res.set('Access-Control-Allow-Origin', '*');
    if (req.method === 'OPTIONS') {
        res.set('Access-Control-Allow-Methods', 'POST');
        res.set('Access-Control-Allow-Headers', 'Content-Type');
        res.status(204).send('');
        return;
    }

    const { bookingId, amount, customerName, courtName, bookingDate } = req.body;
    if (!bookingId || !amount) {
        return res.status(400).json({ success: false, error: 'Missing required fields' });
    }

    const depositAmount = Math.round(amount * 0.30);

    // orderCode phải là số nguyên dương, unique
    const orderCode = parseInt(Date.now().toString().slice(-9));

    const description = `Coc san ${bookingId.slice(-6)}`;

    const cancelUrl  = 'https://picklebook.app/payment/cancel';
    const returnUrl  = 'https://picklebook.app/payment/success';

    // Tạo chữ ký HMAC-SHA256
    const dataStr = `amount=${depositAmount}&cancelUrl=${cancelUrl}&description=${description}&orderCode=${orderCode}&returnUrl=${returnUrl}`;
    const signature = crypto
        .createHmac('sha256', PAYOS_CHECKSUM)
        .update(dataStr)
        .digest('hex');

    const payload = {
        orderCode,
        amount:      depositAmount,
        description,
        cancelUrl,
        returnUrl,
        signature,
        items: [{
            name:     `Đặt sân ${courtName || 'Pickleball'}`,
            quantity: 1,
            price:    depositAmount
        }],
        buyerName:  customerName || 'Khách hàng',
        expiredAt:  Math.floor(Date.now() / 1000) + 900 // 15 phút
    };

    try {
        const response = await axios.post(
            'https://api-merchant.payos.vn/v2/payment-requests',
            payload,
            {
                headers: {
                    'x-client-id':  PAYOS_CLIENT_ID,
                    'x-api-key':    PAYOS_API_KEY,
                    'Content-Type': 'application/json'
                }
            }
        );

        const { checkoutUrl, qrCode, paymentLinkId } = response.data.data;

        // Cập nhật Booking trên Firestore
        const admin = require('firebase-admin');
        await admin.firestore()
            .collection('Bookings')
            .doc(bookingId)
            .update({
                payosOrderCode:     orderCode,
                payosPaymentLinkId: paymentLinkId,
                depositAmount,
                paymentStatus:      'pending',
                paymentExpiredAt:   Date.now() + 900_000
            });

        return res.json({ success: true, qrCode, checkoutUrl, orderCode, depositAmount });

    } catch (error) {
        console.error('PayOS error:', error.response?.data || error.message);
        return res.status(500).json({ success: false, error: 'Lỗi tạo QR thanh toán' });
    }
};
