package io.instana.slo.ui.slodetail;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import io.instana.slo.data.model.Slo;
import io.instana.slo.data.model.SloReport;
import io.instana.slo.data.repository.SloRepository;

/**
 * ViewModel for the SLO detail screen
 */
public class SloDetailViewModel extends AndroidViewModel {
    private final SloRepository repository;
    private final MutableLiveData<Slo> currentSlo;
    private LiveData<SloRepository.Result<SloReport>> reportLiveData;

    public SloDetailViewModel(@NonNull Application application) {
        super(application);
        repository = SloRepository.getInstance(application);
        currentSlo = new MutableLiveData<>();
    }

    /**
     * Set the current SLO being viewed
     */
    public void setSlo(Slo slo) {
        currentSlo.setValue(slo);
    }

    /**
     * Get the current SLO
     */
    public LiveData<Slo> getCurrentSlo() {
        return currentSlo;
    }

    /**
     * Load the detailed report for the current SLO
     */
    public LiveData<SloRepository.Result<SloReport>> loadReport(String sloId) {
        reportLiveData = repository.getSloReport(sloId);
        return reportLiveData;
    }

    /**
     * Get the report LiveData
     */
    public LiveData<SloRepository.Result<SloReport>> getReportLiveData() {
        return reportLiveData;
    }

    /**
     * Refresh the report
     */
    public void refreshReport() {
        Slo slo = currentSlo.getValue();
        if (slo != null) {
            loadReport(slo.getId());
        }
    }
}
