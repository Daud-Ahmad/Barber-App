package com.qtt.thebarber;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.qtt.thebarber.Adapter.MyHistoryAdapter;
import com.qtt.thebarber.Common.Common;
import com.qtt.thebarber.Common.LoadingDialog;
import com.qtt.thebarber.EventBus.HistoryLoadEvent;
import com.qtt.thebarber.Model.BookingInformation;
import com.qtt.thebarber.databinding.ActivityHistoryBinding;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    ActivityHistoryBinding binding;
    private LoadingDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        getWindow().setStatusBarColor(this.getResources().getColor(R.color.colorBackground));

        initView();
        loadUserBookingInformation();
    }

    private void loadUserBookingInformation() {
        dialog.show();
        ///User/+841689294631/Booking
        CollectionReference userBookingRef = FirebaseFirestore.getInstance()
                .collection("User")
                .document(Common.currentUser.getPhoneNumber())
                .collection("Booking");


        userBookingRef
//                .whereEqualTo("done", true)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<BookingInformation> bookingInformations = new ArrayList<>();

                        for (DocumentSnapshot documentSnapshot : task.getResult()) {
                            BookingInformation bookingInformation = documentSnapshot.toObject(BookingInformation.class);
                            bookingInformations.add(bookingInformation);
                        }

                        EventBus.getDefault().post(new HistoryLoadEvent(true, bookingInformations));
                    }
                    dialog.dismiss();
                })
                .addOnFailureListener(e -> {
                    EventBus.getDefault().post(new HistoryLoadEvent(false, e.getMessage()));
                    dialog.dismiss();
                });
    }

    private void initView() {
        dialog = new LoadingDialog(this);

        binding.recyclerHistory.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        binding.recyclerHistory.setLayoutManager(layoutManager);

        binding.imgBack.setOnClickListener(v -> finish());
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onHistoryLoadd(HistoryLoadEvent event) {
        if (event.getSuccess()) {
            MyHistoryAdapter myHistoryAdapter = new MyHistoryAdapter(this, event.getBookingInformationList());
            binding.recyclerHistory.setAdapter(myHistoryAdapter);
        } else {
            Toast.makeText(this, event.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}