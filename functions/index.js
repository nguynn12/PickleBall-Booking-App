const admin = require('firebase-admin');
admin.initializeApp();

const { onRequest } = require('firebase-functions/v2/https');
const { onSchedule } = require('firebase-functions/v2/scheduler');

const { createPaymentLink }                           = require('./createPaymentLink');
const { payosWebhook }                                = require('./payosWebhook');
const { cancelExpiredBookings, markCompletedBookings } = require('./cancelExpiredBookings');

const REGION = 'asia-southeast1';

exports.createPaymentLink = onRequest(
    { region: REGION, cors: true, invoker: 'public' },
    createPaymentLink
);

exports.payosWebhook = onRequest(
    { region: REGION, cors: true, invoker: 'public' },
    payosWebhook
);

exports.cancelExpiredBookings = onSchedule(
    { schedule: 'every 5 minutes', region: REGION },
    cancelExpiredBookings
);

exports.markCompletedBookings = onSchedule(
    { schedule: '0 23 * * *', region: REGION, timeZone: 'Asia/Ho_Chi_Minh' },
    markCompletedBookings
);
