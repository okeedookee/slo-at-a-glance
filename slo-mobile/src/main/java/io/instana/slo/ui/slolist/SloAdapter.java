package io.instana.slo.ui.slolist;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import io.instana.slo.R;
import io.instana.slo.data.model.Slo;
import io.instana.slo.data.model.TrafficLightStatus;

/**
 * Adapter for displaying SLOs in a RecyclerView with traffic light visualization
 */
public class SloAdapter extends ListAdapter<Slo, SloAdapter.SloViewHolder> {
    private static final String TAG = "SloAdapter";
    private static final Object PAYLOAD_UPDATE = new Object();
    private final OnSloClickListener clickListener;

    public interface OnSloClickListener {
        void onSloClick(Slo slo);
    }

    public SloAdapter(OnSloClickListener clickListener) {
        super(new SloDiffCallback());
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public SloViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_slo, parent, false);
        return new SloViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SloViewHolder holder, int position) {
        Slo slo = getItem(position);
        Log.d(TAG, "onBindViewHolder called for position " + position + ": " + slo.getName() +
                   " (Status: " + slo.getStatus() + ", Loading: " + slo.getLoadingState() + ")");
        holder.bind(slo, clickListener);
    }

    @Override
    public void onViewRecycled(@NonNull SloViewHolder holder) {
        super.onViewRecycled(holder);
        holder.stopFlashing();
    }

    static class SloViewHolder extends RecyclerView.ViewHolder {
        private final CardView cardView;
        private final ImageView trafficLightIcon;
        private final TextView sloNameText;
        private final TextView entityTypeText;
        private final TextView statusText;
        private final TextView sliSloValuesText;
        private Handler flashHandler;
        private Runnable flashRunnable;

        public SloViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (CardView) itemView;
            trafficLightIcon = itemView.findViewById(R.id.traffic_light_icon);
            sloNameText = itemView.findViewById(R.id.slo_name);
            entityTypeText = itemView.findViewById(R.id.entity_type);
            statusText = itemView.findViewById(R.id.status_text);
            sliSloValuesText = itemView.findViewById(R.id.sli_slo_values);
        }

        public void stopFlashing() {
            if (flashHandler != null && flashRunnable != null) {
                flashHandler.removeCallbacks(flashRunnable);
                flashHandler = null;
                flashRunnable = null;
            }
            trafficLightIcon.setAlpha(1.0f);
        }

        public void bind(Slo slo, OnSloClickListener clickListener) {
            Log.d(TAG, ">>> BIND called for SLO: " + slo.getName());
            Log.d(TAG, "    LoadingState: " + slo.getLoadingState());
            Log.d(TAG, "    Status: " + slo.getStatus());
            if (slo.getReport() != null) {
                Log.d(TAG, "    Report SLI: " + (slo.getReport().getSli() * 100.0) + "%");
                Log.d(TAG, "    Report Target: " + (slo.getReport().getSloTarget() * 100.0) + "%");
            } else {
                Log.d(TAG, "    Report: null");
            }
            
            // Set SLO name
            sloNameText.setText(slo.getName());

            // Set entity type
            if (slo.getEntity() != null && slo.getEntity().getEntityType() != null) {
                String entityType = slo.getEntity().getEntityType();
                entityTypeText.setText(capitalizeFirst(entityType));
                entityTypeText.setVisibility(View.VISIBLE);
            } else {
                entityTypeText.setVisibility(View.GONE);
            }

            // Stop any existing animation
            stopFlashing();

            // Handle loading state
            Slo.LoadingState loadingState = slo.getLoadingState();
            Log.d(TAG, "Binding SLO '" + slo.getName() + "' with state: " + loadingState + ", status: " + slo.getStatus());
            
            if (loadingState == Slo.LoadingState.LOADING || loadingState == Slo.LoadingState.NOT_LOADED) {
                // Start flashing animation for both LOADING and NOT_LOADED states
                startFlashingAnimation();
                statusText.setText("Loading...");
                statusText.setTextColor(ContextCompat.getColor(itemView.getContext(),
                        android.R.color.darker_gray));
                sliSloValuesText.setVisibility(View.GONE);
            } else if (loadingState == Slo.LoadingState.FAILED) {
                // Show unknown status
                Log.d(TAG, "SLO '" + slo.getName() + "' loading FAILED - showing gray");
                trafficLightIcon.setImageResource(R.drawable.ic_traffic_light_gray);
                statusText.setText("Unknown");
                statusText.setTextColor(ContextCompat.getColor(itemView.getContext(),
                        android.R.color.darker_gray));
                sliSloValuesText.setVisibility(View.GONE);
            } else {
                // Set traffic light status based on SLO status
                TrafficLightStatus status = slo.getStatus();
                if (status != null) {
                    Log.d(TAG, "SLO '" + slo.getName() + "' LOADED successfully - showing status: " + status);
                    setTrafficLightStatus(status);
                    
                    // Display SLI and SLO target values
                    if (slo.getReport() != null) {
                        // Convert decimal to percentage (e.g., 0.995 -> 99.5%)
                        double sli = slo.getReport().getSli() * 100.0;
                        double sloTarget = slo.getReport().getSloTarget() * 100.0;
                        String valuesText = String.format("%.2f%% / %.2f%%", sli, sloTarget);
                        sliSloValuesText.setText(valuesText);
                        sliSloValuesText.setVisibility(View.VISIBLE);
                    } else {
                        sliSloValuesText.setVisibility(View.GONE);
                    }
                } else {
                    Log.d(TAG, "SLO '" + slo.getName() + "' LOADED but status is null - showing gray");
                    trafficLightIcon.setImageResource(R.drawable.ic_traffic_light_gray);
                    statusText.setText("Unknown");
                    statusText.setTextColor(ContextCompat.getColor(itemView.getContext(),
                            android.R.color.darker_gray));
                    sliSloValuesText.setVisibility(View.GONE);
                }
            }

            // Set click listener
            cardView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onSloClick(slo);
                }
            });

            // Add ripple effect
            cardView.setClickable(true);
            cardView.setFocusable(true);
        }

        private void startFlashingAnimation() {
            final int[] currentColor = {0}; // 0=red, 1=yellow, 2=green
            final int[] drawables = {
                R.drawable.ic_traffic_light_red,
                R.drawable.ic_traffic_light_yellow,
                R.drawable.ic_traffic_light_green
            };

            flashHandler = new Handler(Looper.getMainLooper());
            flashRunnable = new Runnable() {
                @Override
                public void run() {
                    trafficLightIcon.setImageResource(drawables[currentColor[0]]);
                    currentColor[0] = (currentColor[0] + 1) % 3;
                    if (flashHandler != null) {
                        flashHandler.postDelayed(this, 300); // Flash every 300ms
                    }
                }
            };
            flashHandler.post(flashRunnable);
        }

        private void setTrafficLightStatus(TrafficLightStatus status) {
            int iconRes;
            int colorRes;
            String statusText;

            switch (status) {
                case GREEN:
                    iconRes = R.drawable.ic_traffic_light_green;
                    colorRes = R.color.status_green;
                    statusText = itemView.getContext().getString(R.string.status_healthy);
                    break;
                case YELLOW:
                    iconRes = R.drawable.ic_traffic_light_yellow;
                    colorRes = R.color.status_yellow;
                    statusText = itemView.getContext().getString(R.string.status_warning);
                    break;
                case RED:
                    iconRes = R.drawable.ic_traffic_light_red;
                    colorRes = R.color.status_red;
                    statusText = itemView.getContext().getString(R.string.status_critical);
                    break;
                default:
                    // This should not happen as null is handled separately
                    iconRes = R.drawable.ic_traffic_light_gray;
                    colorRes = android.R.color.darker_gray;
                    statusText = "Loading...";
                    break;
            }

            trafficLightIcon.setImageResource(iconRes);
            this.statusText.setText(statusText);
            this.statusText.setTextColor(ContextCompat.getColor(itemView.getContext(), colorRes));
        }

        private String capitalizeFirst(String text) {
            if (text == null || text.isEmpty()) {
                return text;
            }
            return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
        }
    }

    static class SloDiffCallback extends DiffUtil.ItemCallback<Slo> {
        @Override
        public boolean areItemsTheSame(@NonNull Slo oldItem, @NonNull Slo newItem) {
            boolean same = oldItem.getId().equals(newItem.getId());
            Log.d(TAG, "DiffUtil.areItemsTheSame: " + oldItem.getName() + " = " + same +
                       " (oldItem==newItem: " + (oldItem == newItem) + ")");
            return same;
        }

        @Override
        public boolean areContentsTheSame(@NonNull Slo oldItem, @NonNull Slo newItem) {
            // IMPORTANT: When oldItem == newItem (same object reference), we modify the object in place
            // So we can't reliably compare fields. Always return false to force rebinding.
            if (oldItem == newItem) {
                Log.d(TAG, "DiffUtil.areContentsTheSame for '" + oldItem.getName() + "': false (same object, forcing rebind)");
                return false;
            }
            
            // Different objects - compare their contents
            boolean statusEquals = (oldItem.getStatus() == null && newItem.getStatus() == null) ||
                                   (oldItem.getStatus() != null && oldItem.getStatus().equals(newItem.getStatus()));
            boolean loadingStateEquals = oldItem.getLoadingState() == newItem.getLoadingState();
            
            // Check if entity information has changed
            boolean entityEquals = areEntitiesEqual(oldItem.getEntity(), newItem.getEntity());
            
            // Check if report data has changed (important for SLI/SLO values)
            boolean reportEquals = areReportsEqual(oldItem.getReport(), newItem.getReport());
            
            boolean contentsEqual = oldItem.getId().equals(newItem.getId()) &&
                   oldItem.getName().equals(newItem.getName()) &&
                   statusEquals &&
                   loadingStateEquals &&
                   entityEquals &&
                   reportEquals;
            
            // Log for debugging
            Log.d(TAG, "DiffUtil.areContentsTheSame for '" + oldItem.getName() + "': " + contentsEqual);
            
            return contentsEqual;
        }
        
        @Override
        public Object getChangePayload(@NonNull Slo oldItem, @NonNull Slo newItem) {
            // Return a payload to force rebinding when content changes
            // This is necessary because we modify Slo objects in place
            return PAYLOAD_UPDATE;
        }
        
        private boolean areEntitiesEqual(io.instana.slo.data.model.SloEntity oldEntity,
                                        io.instana.slo.data.model.SloEntity newEntity) {
            // Both null
            if (oldEntity == null && newEntity == null) {
                return true;
            }
            // One is null, other is not
            if (oldEntity == null || newEntity == null) {
                return false;
            }
            // Compare entity types
            String oldType = oldEntity.getEntityType();
            String newType = newEntity.getEntityType();
            
            if (oldType == null && newType == null) {
                return true;
            }
            if (oldType == null || newType == null) {
                return false;
            }
            return oldType.equals(newType);
        }
        
        private boolean areReportsEqual(io.instana.slo.data.model.SloReport oldReport,
                                       io.instana.slo.data.model.SloReport newReport) {
            // Both null
            if (oldReport == null && newReport == null) {
                return true;
            }
            // One is null, other is not - this is a change!
            if (oldReport == null || newReport == null) {
                return false;
            }
            // Compare report values
            return oldReport.getSli() == newReport.getSli() &&
                   oldReport.getSloTarget() == newReport.getSloTarget() &&
                   oldReport.getErrorBudgetRemaining() == newReport.getErrorBudgetRemaining();
        }
    }
}
