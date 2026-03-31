const {onValueUpdated} = require('firebase-functions/v2/database');
const {onValueCreated} = require('firebase-functions/v2/database');
const admin = require('firebase-admin');
admin.initializeApp();

// ✅ Queue notification
exports.sendQueueNotification = onValueUpdated(
    {
        ref: '/queue/active/{queueId}',
        region: 'asia-southeast1',
        instance: 'uptm-smartqueue-default-rtdb'
    },
    async (event) => {
        const before = event.data.before.val();
        const after = event.data.after.val();

        if (before.status !== 'serving' && after.status === 'serving') {
            const studentId = after.studentId;
            const queueNumber = after.queueNumber;
            const counter = after.counter;

            const tokenSnapshot = await admin.database()
                .ref(`students/${studentId}/fcmToken`)
                .once('value');

            const fcmToken = tokenSnapshot.val();
            if (!fcmToken) return null;

            const message = {
                token: fcmToken,
                android: { priority: 'high' },
                data: {
                    type: 'queue',
                    title: '🔔 Your Turn!',
                    body: `Queue ${queueNumber} - Please proceed to ${counter} now!`
                }
            };

            try {
                await admin.messaging().send(message);
                console.log('✅ Queue notification sent to:', studentId);
            } catch (error) {
                console.error('❌ Error:', error);
            }
        }
        return null;
    }
);

// ✅ Chat notification bila staff reply
exports.sendChatNotification = onValueCreated(
    {
        ref: '/messages/{chatId}/{messageId}',
        region: 'asia-southeast1',
        instance: 'uptm-smartqueue-default-rtdb'
    },
    async (event) => {
        const message = event.data.val();

        if (message.senderType !== 'staff') return null;
        if (message.senderId === 'system') return null;

        const chatSnapshot = await admin.database()
            .ref(`chats/${event.params.chatId}`)
            .once('value');

        const chat = chatSnapshot.val();
        if (!chat) return null;

        const studentId = chat.studentId;

        const tokenSnapshot = await admin.database()
            .ref(`students/${studentId}/fcmToken`)
            .once('value');

        const fcmToken = tokenSnapshot.val();
        if (!fcmToken) {
            console.log('No FCM token for student:', studentId);
            return null;
        }

        const fcmMessage = {
            token: fcmToken,
            android: { priority: 'high' },
            data: {
                type: 'chat',
                title: '💬 New Message from Bursar',
                body: message.message.length > 50
                    ? message.message.substring(0, 50) + '...'
                    : message.message
            }
        };

        try {
            await admin.messaging().send(fcmMessage);
            console.log('✅ Chat notification sent to:', studentId);
        } catch (error) {
            console.error('❌ Error sending chat notification:', error);
        }

        return null;
    }
);

// ✅ Announcement notification
exports.sendAnnouncementNotification = onValueCreated(
    {
        ref: '/announcements/{announcementId}',
        region: 'asia-southeast1',
        instance: 'uptm-smartqueue-default-rtdb'
    },
    async (event) => {
        const announcement = event.data.val();

        const studentsSnapshot = await admin.database()
            .ref('students')
            .once('value');

        const uniqueTokens = new Set();

        studentsSnapshot.forEach(studentSnap => {
            const student = studentSnap.val();
            const fcmToken = student.fcmToken;
            if (fcmToken) {
                uniqueTokens.add(fcmToken);
            }
        });

        console.log(`📢 Sending to ${uniqueTokens.size} unique devices`);

        const sendPromises = [];

        uniqueTokens.forEach(token => {
            const message = {
                token: token,
                android: { priority: 'high' },
                data: {
                    type: 'announcement',
                    title: `📢 ${announcement.title}`,
                    body: announcement.message.length > 50
                        ? announcement.message.substring(0, 50) + '...'
                        : announcement.message
                }
            };

            sendPromises.push(
                admin.messaging().send(message)
                    .catch(err => console.error('❌ Error:', err))
            );
        });

        await Promise.all(sendPromises);
        console.log('✅ Announcement sent!');
        return null;
    }
);

// ✅ Recall notification
exports.sendRecallNotification = onValueUpdated(
    {
        ref: '/counters/{counter}',
        region: 'asia-southeast1',
        instance: 'uptm-smartqueue-default-rtdb'
    },
    async (event) => {
        const before = event.data.before.val();
        const after = event.data.after.val();

        // Trigger only bila recalled bertukar dari false/null ke true
        if (!before.recalled && after.recalled === true) {
            const counter = event.params.counter;
            const queueNumber = after.currentServing;

            if (!queueNumber) return null;

            // Cari student yang sedang serving kat counter ni
            const queueSnapshot = await admin.database()
                .ref('queue/active')
                .orderByChild('counter')
                .equalTo(counter)
                .once('value');

            let studentId = null;

            queueSnapshot.forEach(child => {
                const q = child.val();
                if (q.status === 'serving' && q.queueNumber === queueNumber) {
                    studentId = q.studentId;
                }
            });

            if (!studentId) {
                console.log('❌ Student not found for recall');
                return null;
            }

            // Ambil FCM token student
            const tokenSnapshot = await admin.database()
                .ref(`students/${studentId}/fcmToken`)
                .once('value');

            const fcmToken = tokenSnapshot.val();
            if (!fcmToken) {
                console.log('❌ No FCM token for student:', studentId);
                return null;
            }

            const message = {
                token: fcmToken,
                android: { priority: 'high' },
                data: {
                    type: 'recall',
                    title: '📢 Please Proceed Now!',
                    body: `Queue ${queueNumber} - You are being recalled to ${counter}. Please come immediately!`
                }
            };

            try {
                await admin.messaging().send(message);
                console.log('✅ Recall notification sent to:', studentId);
            } catch (error) {
                console.error('❌ Error sending recall notification:', error);
            }
        }

        return null;
    }
);