package com.example.myapplication555;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.myapplication555.databinding.FragmentFirstBinding;

public class FirstFragment extends Fragment {

    private FragmentFirstBinding binding;
    private MainActivity mainActivity;
    private Button controlButton;
    private TextView statusText;
    private TextView ipAddressText;
    private TextView connectionInfo;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof MainActivity) {
            mainActivity = (MainActivity) context;
        }
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentFirstBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize UI elements
        controlButton = binding.controlButton;
        statusText = binding.statusText;
        ipAddressText = binding.ipAddressText;
        connectionInfo = binding.connectionInfo;

        // Register this fragment with MainActivity
        if (mainActivity != null) {
            mainActivity.setFirstFragment(this);
        }

        // Set up button click listeners
        controlButton.setOnClickListener(v -> {
            if (mainActivity != null) {
                mainActivity.toggleRemoteControlFromFragment();
            }
        });

        binding.buttonFirst.setOnClickListener(v ->
                NavHostFragment.findNavController(FirstFragment.this)
                        .navigate(R.id.action_FirstFragment_to_SecondFragment)
        );

        // Initialize the display
        updateRemoteControlStatus(false, getString(R.string.device_ip_unknown));
    }

    /**
     * Update the remote control status display
     * @param isRunning Whether the remote control is running
     * @param ipAddress The device IP address
     */
    public void updateRemoteControlStatus(boolean isRunning, String ipAddress) {
        if (statusText != null && ipAddressText != null && controlButton != null) {
            if (isRunning) {
                statusText.setText(getString(R.string.status_running));
                statusText.setTextColor(getResources().getColor(R.color.status_running, null));
                controlButton.setText(getString(R.string.remote_control_button_stop));
                
                // Format IP address
                String formattedIp = getString(R.string.device_ip_format, ipAddress);
                ipAddressText.setText(formattedIp);
                ipAddressText.setTextColor(getResources().getColor(R.color.success_green, null));
                
                // Show connection info
                if (connectionInfo != null) {
                    connectionInfo.setVisibility(View.VISIBLE);
                }
            } else {
                statusText.setText(getString(R.string.status_stopped));
                statusText.setTextColor(getResources().getColor(R.color.status_stopped, null));
                controlButton.setText(getString(R.string.remote_control_button_start));
                
                ipAddressText.setText(getString(R.string.device_ip_unknown));
                ipAddressText.setTextColor(getResources().getColor(R.color.text_tertiary, null));
                
                // Hide connection info
                if (connectionInfo != null) {
                    connectionInfo.setVisibility(View.GONE);
                }
            }
        }
    }

    /**
     * Update status to pending (starting)
     */
    public void updateStatusToPending() {
        if (statusText != null && controlButton != null) {
            statusText.setText(getString(R.string.status_pending));
            statusText.setTextColor(getResources().getColor(R.color.status_pending, null));
            controlButton.setEnabled(false);
        }
    }

    /**
     * Re-enable the control button after operation
     */
    public void enableControlButton() {
        if (controlButton != null) {
            controlButton.setEnabled(true);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}