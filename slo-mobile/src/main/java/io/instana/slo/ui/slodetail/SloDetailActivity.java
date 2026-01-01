package io.instana.slo.ui.slodetail;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import io.instana.slo.R;
import io.instana.slo.data.model.ChartDataPoint;
import io.instana.slo.data.model.SloReport;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Activity displaying detailed SLO report with metrics and chart
 */
public class SloDetailActivity extends AppCompatActivity {
    public static final String EXTRA_SLO_ID = "slo_id";
    public static final String EXTRA_SLO_NAME = "slo_name";
    public static final String EXTRA_TIME_WINDOW = "time_window";

    private SloDetailViewModel viewModel;
    private String sloId;
    private String sloName;
    private io.instana.slo.data.model.TimeWindow timeWindow;

    // Views
    private TextView sloNameText;
    private TextView sliValueText;
    private TextView sloTargetText;
    private TextView totalErrorBudgetText;
    private TextView errorBudgetRemainingText;
    private TextView errorBudgetPercentageText;
    private TextView timeRangeText;
    private TextView timeWindowTypeText;
    private TextView timeWindowSizeText;
    private LineChart errorBudgetChart;
    private ProgressBar progressBar;
    private View contentView;
    private TextView errorView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_slo_detail);

        // Enable back button
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.slo_detail_title);
        }

        // Get extras
        sloId = getIntent().getStringExtra(EXTRA_SLO_ID);
        sloName = getIntent().getStringExtra(EXTRA_SLO_NAME);
        timeWindow = (io.instana.slo.data.model.TimeWindow) getIntent().getSerializableExtra(EXTRA_TIME_WINDOW);

        if (sloId == null) {
            Toast.makeText(this, R.string.error_invalid_slo, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize views
        initViews();

        // Set up ViewModel
        viewModel = new ViewModelProvider(this).get(SloDetailViewModel.class);

        // Load SLO report
        loadSloReport();
    }

    private void initViews() {
        sloNameText = findViewById(R.id.slo_name);
        sliValueText = findViewById(R.id.sli_value);
        sloTargetText = findViewById(R.id.slo_target);
        totalErrorBudgetText = findViewById(R.id.total_error_budget);
        errorBudgetRemainingText = findViewById(R.id.error_budget_remaining);
        errorBudgetPercentageText = findViewById(R.id.error_budget_percentage);
        timeRangeText = findViewById(R.id.time_range);
        timeWindowTypeText = findViewById(R.id.time_window_type);
        timeWindowSizeText = findViewById(R.id.time_window_size);
        errorBudgetChart = findViewById(R.id.error_budget_chart);
        progressBar = findViewById(R.id.progress_bar);
        contentView = findViewById(R.id.content_view);
        errorView = findViewById(R.id.error_view);

        // Set SLO name
        if (sloName != null) {
            sloNameText.setText(sloName);
        }

        // Configure chart
        configureChart();
    }

    private void loadSloReport() {
        viewModel.loadReport(sloId).observe(this, result -> {
            if (result != null) {
                switch (result.status) {
                    case LOADING:
                        showLoading(true);
                        hideError();
                        break;
                    case SUCCESS:
                        showLoading(false);
                        hideError();
                        if (result.data != null) {
                            displayReport(result.data);
                        }
                        break;
                    case ERROR:
                        showLoading(false);
                        showError(result.message);
                        break;
                }
            }
        });
    }

    private void displayReport(SloReport report) {
        // Display SLI
        sliValueText.setText(String.format(Locale.getDefault(), "%.2f%%", report.getSli() * 100));

        // Display SLO target
        sloTargetText.setText(String.format(Locale.getDefault(), "%.2f%%", report.getSloTarget() * 100));

        // Display total error budget
        totalErrorBudgetText.setText(String.format(Locale.getDefault(), "%.2f", report.getTotalErrorBudget()));

        // Display error budget remaining
        errorBudgetRemainingText.setText(String.format(Locale.getDefault(), "%.2f", report.getErrorBudgetRemaining()));

        // Display time range in a single line with shorter format
        SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        String fromTime = dateTimeFormat.format(new Date(report.getFromTimestamp()));
        String toTime = dateTimeFormat.format(new Date(report.getToTimestamp()));
        timeRangeText.setText(fromTime + " - " + toTime);

        // Display time window information from the SLO configuration (passed via intent)
        if (timeWindow != null) {
            timeWindowTypeText.setText(timeWindow.getFormattedType());
            timeWindowSizeText.setText(timeWindow.getFormattedSize());
        } else {
            timeWindowTypeText.setText("N/A");
            timeWindowSizeText.setText("N/A");
        }

        // Display error budget percentage (ensure it's not negative)
        double percentage = report.getErrorBudgetRemainingPercentage();
        if (percentage < 0) {
            percentage = 0;
        }
        errorBudgetPercentageText.setText(String.format(Locale.getDefault(), "%.1f%%", percentage));

        // Set color based on percentage
        int color;
        if (percentage > 50) {
            color = ContextCompat.getColor(this, R.color.status_green);
        } else if (percentage > 20) {
            color = ContextCompat.getColor(this, R.color.status_yellow);
        } else {
            color = ContextCompat.getColor(this, R.color.status_red);
        }
        errorBudgetPercentageText.setTextColor(color);

        // Display chart
        if (report.getErrorBudgetRemainChart() != null && !report.getErrorBudgetRemainChart().isEmpty()) {
            displayChart(report.getErrorBudgetRemainChart());
        }

        contentView.setVisibility(View.VISIBLE);
    }

    private void configureChart() {
        errorBudgetChart.getDescription().setEnabled(false);
        errorBudgetChart.setTouchEnabled(true);
        errorBudgetChart.setDragEnabled(true);
        errorBudgetChart.setScaleEnabled(true);
        errorBudgetChart.setPinchZoom(true);
        errorBudgetChart.setDrawGridBackground(false);

        // X-axis configuration
        XAxis xAxis = errorBudgetChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(true);
        xAxis.setGranularity(1f);
        xAxis.setLabelRotationAngle(-90f);
        xAxis.setValueFormatter(new ValueFormatter() {
            private final SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd", Locale.getDefault());

            @Override
            public String getFormattedValue(float value) {
                return dateFormat.format(new Date((long) value));
            }
        });

        // Y-axis configuration
        YAxis leftAxis = errorBudgetChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setAxisMinimum(0f);

        YAxis rightAxis = errorBudgetChart.getAxisRight();
        rightAxis.setEnabled(false);

        // Legend
        errorBudgetChart.getLegend().setEnabled(true);
    }

    private void displayChart(List<ChartDataPoint> dataPoints) {
        List<Entry> entries = new ArrayList<>();

        for (ChartDataPoint point : dataPoints) {
            entries.add(new Entry(point.getTimestamp(), (float) point.getValue()));
        }

        LineDataSet dataSet = new LineDataSet(entries, getString(R.string.error_budget_remaining));
        dataSet.setColor(ContextCompat.getColor(this, R.color.primary));
        dataSet.setLineWidth(2f);
        dataSet.setCircleColor(ContextCompat.getColor(this, R.color.primary));
        dataSet.setCircleRadius(3f);
        dataSet.setDrawCircleHole(false);
        dataSet.setDrawValues(false);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(ContextCompat.getColor(this, R.color.primary_light));
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        LineData lineData = new LineData(dataSet);
        errorBudgetChart.setData(lineData);
        errorBudgetChart.invalidate();
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        contentView.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void showError(String message) {
        errorView.setText(message);
        errorView.setVisibility(View.VISIBLE);
        contentView.setVisibility(View.GONE);
    }

    private void hideError() {
        errorView.setVisibility(View.GONE);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
