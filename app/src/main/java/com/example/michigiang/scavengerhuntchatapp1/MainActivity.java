package com.example.michigiang.scavengerhuntchatapp1;
/**
 * Half of this code is copyrighted to Google Inc. All Rights Reserved.
 * Some methods are copied from Udacity's FriendlyChat app,
 * some are written by following Udacity tutorial
 *
 * Michelle Giang giang2
 */

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.CountDownTimer;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private long time = 1800000;
    private CountDownTimer countDownTimer;

    private static final String TAG = "MainActivity";

    public static final String ANONYMOUS = "anonymous";
    public static final int DEFAULT_MSG_LENGTH_LIMIT = 1000;
    private static final int RC_PHOTO_PICKER =  2;

    private ListView mMessageListView;
    private MessageAdapter mMessageAdapter;
    private ProgressBar mProgressBar;
    private ImageButton mPhotoPickerButton;
    private EditText mMessageEditText;
    private Button mSendButton;

    private String mUsername;

    //Firebase instance variables
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mMessagesDatabaseReference;
    private DatabaseReference mCountDatabaseReference;
    private DatabaseReference mListDatabaseReference;
    private DatabaseReference mTimerDatabaseReference;
    private ChildEventListener mChildEventListener;
    private ValueEventListener mCountEventListener;
    private FirebaseStorage mFirebaseStorage;
    private StorageReference mChatPhotosStorageReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mUsername = getIntent().getStringExtra("username");
        String room = getIntent().getStringExtra("room");

        //Initialize firebase components
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mFirebaseStorage = FirebaseStorage.getInstance();
        mMessagesDatabaseReference = mFirebaseDatabase.getReference(room).child("messages");
        mChatPhotosStorageReference = mFirebaseStorage.getReference(room).child("chat_photos");
        mCountDatabaseReference = mFirebaseDatabase.getReference(room).child("count");
        mListDatabaseReference = mFirebaseDatabase.getReference(room).child("list");
        mTimerDatabaseReference = mFirebaseDatabase.getReference(room).child("timer");

        // Initialize references to views
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mMessageListView = (ListView) findViewById(R.id.messageListView);
        mPhotoPickerButton = (ImageButton) findViewById(R.id.photoPickerButton);
        mMessageEditText = (EditText) findViewById(R.id.messageEditText);
        mSendButton = (Button) findViewById(R.id.sendButton);

        // Initialize message ListView and its adapter
        List<FriendlyMessage> friendlyMessages = new ArrayList<>();
        mMessageAdapter = new MessageAdapter(this, R.layout.item_message, friendlyMessages);
        mMessageListView.setAdapter(mMessageAdapter);

        // Initialize progress bar
        mProgressBar.setVisibility(ProgressBar.INVISIBLE);

        //Initializes all the buttons and edittexts
        final EditText listText = (EditText) findViewById(R.id.listText);
        final Button finalizeButton = (Button) findViewById(R.id.finalize_button);
        final EditText timerText = (EditText) findViewById(R.id.timerText);
        final Button startButton = (Button) findViewById(R.id.start_button);
        final Button stopButton = (Button) findViewById(R.id.stop_button);

        mCountDatabaseReference.child(mUsername).setValue(mUsername);

        //Puts username in firebase
        mCountDatabaseReference.addListenerForSingleValueEvent(mCountEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                int peopleCount = 0;

                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    peopleCount++;
                }

                //If the user is not the first person to enter the room,
                // then he/she cannot write the list nor finalize it
                if (peopleCount != 1) {
                    listText.setFocusable(false);
                    listText.setKeyListener(null);
                    finalizeButton.setEnabled(false);
                } else {
                    timerText.setText("" + String.format("%d : %d",
                            TimeUnit.MILLISECONDS.toMinutes(time),
                            TimeUnit.MILLISECONDS.toSeconds(0) -
                                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(0))));
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        //Makes the list uneditable and the finalize button disabled after the finalize button is clicked
        //The timer buttons are then enabled
        finalizeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mListDatabaseReference.setValue(listText.getText().toString());
                listText.setFocusable(false);
                listText.setKeyListener(null);
                finalizeButton.setEnabled(false);
                startButton.setEnabled(true);
                stopButton.setEnabled(true);
            }
        });

        //When the leader finalizes the list, it adds it to the database and the event is handled
        mListDatabaseReference.addValueEventListener(new ValueEventListener() {
             @Override
             public void onDataChange(DataSnapshot dataSnapshot) {
                 //If the user is not the leader and the list is finalized,
                 //then the list will appear on his/her device
                 if (!finalizeButton.isEnabled()) {
                     listText.setText("" + dataSnapshot.getValue());
                 }
             }

             @Override
             public void onCancelled(DatabaseError databaseError) {

             }
         });


        //Initializes the timer and makes it uneditable
        timerText.setFocusable(false);
        timerText.setText("" + String.format("%d : %d",
                TimeUnit.MILLISECONDS.toMinutes(time),
                TimeUnit.MILLISECONDS.toSeconds(0) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(0))));

        //Starts the timer when start button is clicked
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Starts the timer and changes the button text to pause. It records the time each time it changes
                if (startButton.getText().equals("START")) {
                    startButton.setText("PAUSE");
                    countDownTimer = new CountDownTimer(time, 1000) {
                        public void onTick(long millisUntilFinished) {
                            timerText.setText("" + String.format("%d : %d",
                                    TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished),
                                    TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) -
                                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished))));
                            mTimerDatabaseReference.setValue("time");
                            mTimerDatabaseReference.child("time").setValue("" + String.format("%d : %d",
                                    TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished),
                                    TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) -
                                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished))));
                            time = millisUntilFinished;
                        }

                        //When the timer is finished, the buttons are disabled and no one can chat
                        public void onFinish() {
                            startButton.setEnabled(false);
                            stopButton.setEnabled(false);
                            mTimerDatabaseReference.child("time").setValue("" + String.format("%d : %d",
                                    TimeUnit.MILLISECONDS.toMinutes(time),
                                    TimeUnit.MILLISECONDS.toSeconds(0) -
                                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(0))));
                            timerText.setText("" + String.format("%d : %d",
                                    TimeUnit.MILLISECONDS.toMinutes(time),
                                    TimeUnit.MILLISECONDS.toSeconds(0) -
                                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(0))));
                            mMessageEditText.setFocusable(false);
                            mMessageEditText.setKeyListener(null);
                            mPhotoPickerButton.setEnabled(false);
                            mListDatabaseReference.removeValue();
                            listText.getText().clear();
                        }
                    }.start();
                } else {
                    //Timer pauses and changes the button text to start
                    startButton.setText("START");
                    countDownTimer.cancel();
                }
            }
        });

        //Stops the timer and resets it
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                countDownTimer.cancel();
                startButton.setText("START");
                startButton.setEnabled(false);
                stopButton.setEnabled(false);
                finalizeButton.setEnabled(true);
                listText.setFocusable(true);

                time = 1800000;
                mTimerDatabaseReference.child("time").setValue("" + String.format("%d : %d",
                        TimeUnit.MILLISECONDS.toMinutes(time),
                        TimeUnit.MILLISECONDS.toSeconds(0) -
                                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(0))));
                timerText.setText("" + String.format("%d : %d",
                        TimeUnit.MILLISECONDS.toMinutes(time),
                        TimeUnit.MILLISECONDS.toSeconds(0) -
                                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(0))));
                mListDatabaseReference.removeValue();
                listText.getText().clear();
            }
        });

        //Constantly updates the time of other players
        mTimerDatabaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!startButton.isEnabled()) {
                    timerText.setText("" + dataSnapshot.getValue());
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        // ImagePickerButton shows an image picker to upload a image for a message
        mPhotoPickerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/jpeg");
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                startActivityForResult(Intent.createChooser(intent, "Complete action using"), RC_PHOTO_PICKER);
            }
        });

        // Enable Send button when there's text to send
        mMessageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().length() > 0) {
                    mSendButton.setEnabled(true);
                } else {
                    mSendButton.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
        mMessageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(DEFAULT_MSG_LENGTH_LIMIT)});

        // Send button sends a message and clears the EditText
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FriendlyMessage friendlyMessage = new FriendlyMessage(mMessageEditText.getText().toString(), mUsername, null);
                mMessagesDatabaseReference.push().setValue(friendlyMessage);

                // Clear input box
                mMessageEditText.setText("");
            }
        });

        attachDatabaseReadListener();
    }

    //Handles the action for when someone adds a photo
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_PHOTO_PICKER && resultCode ==  RESULT_OK) {
            Uri selectedImageUri = data.getData();
            StorageReference photoRef = mChatPhotosStorageReference.child(selectedImageUri.getLastPathSegment());
            photoRef.putFile(selectedImageUri).addOnSuccessListener
                    (this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            @SuppressWarnings("VisibleForTests")
                            Uri downloadURL = taskSnapshot.getDownloadUrl();
                            FriendlyMessage friendlyMessage = new FriendlyMessage(null, mUsername, downloadURL.toString());
                            mMessagesDatabaseReference.push().setValue(friendlyMessage);
                        }
                    });
        }
    }

    //Remove databaselistener
    @Override
    protected void onPause() {
        super.onPause();
        if (mMessageAdapter != null) {
            mMessageAdapter.clear();
        }
        detachDatabaseReadListener();
    }

    //Adds messages to the database
    private void attachDatabaseReadListener() {
        if (mChildEventListener == null) {
            mChildEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    FriendlyMessage friendlyMessage = dataSnapshot.getValue(FriendlyMessage.class);
                    mMessageAdapter.add(friendlyMessage);
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {

                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {

                }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            };
            mMessagesDatabaseReference.addChildEventListener(mChildEventListener);
        }
    }

    //Removes the database listener
    private void detachDatabaseReadListener() {
        if (mChildEventListener != null) {
            mMessagesDatabaseReference.removeEventListener(mChildEventListener);
        }
    }

    //Clears the adapter when the android back button is pressed
    @Override
    public void onBackPressed() {
        mMessageAdapter.clear();
        mCountDatabaseReference.child(mUsername).removeValue();
        mCountDatabaseReference.removeEventListener(mCountEventListener);
        super.onBackPressed();
    }
}
