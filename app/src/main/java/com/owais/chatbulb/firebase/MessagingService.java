package com.owais.chatbulb.firebase;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.util.Base64;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.owais.chatbulb.R;
import com.owais.chatbulb.activities.ChatActivity;
import com.owais.chatbulb.activities.ReplyActivity;
import com.owais.chatbulb.models.User;
import com.owais.chatbulb.utilities.Constants;
import com.owais.chatbulb.utilities.PreferenceManager;

import java.util.Random;

public class MessagingService extends FirebaseMessagingService {

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);
        PreferenceManager preferenceManager = new PreferenceManager(getApplicationContext());
        User user = new User();
        user.id = message.getData().get(Constants.KEY_USER_ID);
        user.name = message.getData().get(Constants.KEY_NAME);
        user.token = message.getData().get(Constants.KEY_FCM_TOKEN);


        int notificationId = new Random().nextInt();
        String channelId = "chat_message";

        Intent intent = new Intent(getApplicationContext(), ReplyActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra(Constants.KEY_USER, user);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId);

// Set the small icon (typically the app icon)
        builder.setSmallIcon(R.drawable.ic_notification);

// Set the sender's name as the notification title
        builder.setContentTitle(user.name);

// Set the message text as the notification content
        builder.setContentText(message.getData().get(Constants.KEY_MESSAGE));

// Set the notification style to a BigTextStyle for expandable notifications
        builder.setStyle(new NotificationCompat.BigTextStyle().bigText(
                message.getData().get(Constants.KEY_MESSAGE)
        ));

// Set the notification priority to high
        builder.setPriority(NotificationCompat.PRIORITY_HIGH);

// Set the content intent (what happens when the notification is clicked)
        builder.setContentIntent(pendingIntent);

// Disable auto-cancel to keep the notification visible until explicitly dismissed
        builder.setAutoCancel(false);

// Add a reply action button
        Intent replyIntent = new Intent(this, ReplyActivity.class); // Replace with your reply activity
        PendingIntent replyPendingIntent = PendingIntent.getActivity(this, 0, replyIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Action replyAction = new NotificationCompat.Action.Builder(
                R.drawable.ic_send, // Icon for the action button
                "Reply", // Title for the action button
                replyPendingIntent // PendingIntent for handling the reply action
        ).build();

        builder.addAction(replyAction);

// Optionally, set a unique identifier for each notification to update or cancel them
// builder.setNotificationId(notificationId);

// Finally, build and show the notification
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        notificationManager.notify(notificationId, builder.build());


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence channelName = "Messages";
            String channelDescription = "Chat message notification";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(channelId, channelName, importance);
            channel.setDescription(channelDescription);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getApplicationContext(), "Please provide notification permission", Toast.LENGTH_SHORT).show();
            return;
        }
        notificationManagerCompat.notify(notificationId, builder.build());
    }
}
