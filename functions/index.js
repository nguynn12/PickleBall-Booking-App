const functions = require('firebase-functions');
const admin     = require('firebase-admin');
admin.initializeApp();

const { createPaymentLink }    = require('./createPaymentLink');
const { payosWebhook }         = require('./payosWebhook');
const { cancelExpiredBookings, markCompletedBookings } = require('./cancelExpiredBookings');

// HTTP functions
exports.createPaymentLink = functions
    .region('asia-southeast1')
    .https.onRequest(createPaymentLink);

exports.payosWebhook = functions
    .region('asia-southeast1')
    .https.onRequest(payosWebhook);

// Scheduled functions (cron)
exports.cancelExpiredBookings = functions
    .region('asia-southeast1')
    .pubsub.schedule('every 5 minutes')
    .onRun(cancelExpiredBookings);

exports.markCompletedBookings = functions
    .region('asia-southeast1')
    .pubsub.schedule('0 23 * * *')
    .timeZone('Asia/Ho_Chi_Minh')
    .onRun(markCompletedBookings);
