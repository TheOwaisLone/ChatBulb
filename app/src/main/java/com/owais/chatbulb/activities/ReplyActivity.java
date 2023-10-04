package com.owais.chatbulb.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.Toast;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.owais.chatbulb.R;
import com.owais.chatbulb.databinding.ActivityChatBinding;
import com.owais.chatbulb.models.User;
import com.owais.chatbulb.network.ApiClient;
import com.owais.chatbulb.network.ApiService;
import com.owais.chatbulb.utilities.Constants;
import com.owais.chatbulb.utilities.PreferenceManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.HashMap;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReplyActivity extends AppCompatActivity {
    private ActivityChatBinding binding;
    private PreferenceManager preferenceManager;
    private FirebaseFirestore database;
    private User receiverUser;
    private Boolean isReceiverAvailable = false;
    private String conversationId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setListeners();
    }

    private void sendNotification(String messageBody){
        ApiClient.getClint().create(ApiService.class).sendMessage(
                Constants.getRemoteMsgHeaders(),
                messageBody
        ).enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                if (response.isSuccessful()){
                    try {
                        if (response.body() != null){
                            JSONObject responseJson = new JSONObject(response.body());
                            JSONArray results = responseJson.getJSONArray("results");
                            if (responseJson.getInt("Failure") == 1){
                                JSONObject error = (JSONObject) results.get(0);
                                showToast(error.getString("error"));
                                return;
                            }
                        }
                    }catch (JSONException e){
                        e.printStackTrace();
                    }
                    showToast("Notification sent Successfully");
                }else {
                    showToast("Error Occurred with code :" + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                showToast(t.getMessage());
            }
        });
    }

    private void sendMessage(){
        if (!binding.inputMsg.getText().toString().trim().isEmpty()) {
            HashMap<String, Object> message = new HashMap<>();
            message.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
            message.put(Constants.KEY_RECEIVER_ID, receiverUser.id);
            message.put(Constants.KEY_MESSAGE, binding.inputMsg.getText().toString().trim());
            message.put(Constants.KEY_TIMESTAMP, new Date());
            database.collection(Constants.KEY_COLLECTION_CHAT).add(message);
            if (conversationId != null){
                updateConversation(binding.inputMsg.getText().toString());
            }else {
                HashMap<String, Object> conversation = new HashMap<>();
                conversation.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
                conversation.put(Constants.KEY_SENDER_NAME, preferenceManager.getString(Constants.KEY_NAME));
                conversation.put(Constants.KEY_SENDER_IMAGE, preferenceManager.getString(Constants.KEY_IMAGE));
                conversation.put(Constants.KEY_RECEIVER_ID, receiverUser.id);
                conversation.put(Constants.KEY_RECEIVER_NAME, receiverUser.name);
                conversation.put(Constants.KEY_RECEIVER_IMAGE, receiverUser.image);
                conversation.put(Constants.KEY_LAST_MESSAGE, binding.inputMsg.getText().toString().trim());
                conversation.put(Constants.KEY_TIMESTAMP, new Date());
                addConversation(conversation);
            }
            if (!isReceiverAvailable){
                try {
                    JSONArray tokens = new JSONArray();
                    tokens.put(receiverUser.token);

                    JSONObject data = new JSONObject();
                    data.put(Constants.KEY_USER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
                    data.put(Constants.KEY_NAME, preferenceManager.getString(Constants.KEY_NAME));
                    data.put(Constants.KEY_FCM_TOKEN, preferenceManager.getString(Constants.KEY_FCM_TOKEN));
                    data.put(Constants.KEY_MESSAGE, binding.inputMsg.getText().toString().trim());

                    JSONObject body = new JSONObject();
                    body.put(Constants.REMOTE_MSG_DATA, data);
                    body.put(Constants.REMOTE_MSG_REGISTRATION_IDS, tokens);

                    sendNotification(body.toString());
                }catch (Exception exception){
                    showToast(exception.getMessage());
                }
            }
            binding.inputMsg.setText(null);
        }
    }



    private void showToast(String msg){
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }


    private void addConversation(HashMap<String, Object> conversation){
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .add(conversation)
                .addOnSuccessListener(documentReference -> conversationId = documentReference.getId());
    }

    private void updateConversation(String message){
        DocumentReference documentReference =
                database.collection(Constants.KEY_COLLECTION_CONVERSATIONS).document(conversationId);
        documentReference.update(
                Constants.KEY_LAST_MESSAGE, message,
                Constants.KEY_TIMESTAMP, new Date()
        );
    }

    private void setListeners() {
        binding.layoutSend.setOnClickListener(v -> sendMessage());
    }

}