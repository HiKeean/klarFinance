package com.klarfinance.app.core.location;

import androidx.hilt.work.WorkerAssistedFactory;
import androidx.work.ListenableWorker;
import dagger.Binds;
import dagger.Module;
import dagger.hilt.InstallIn;
import dagger.hilt.codegen.OriginatingElement;
import dagger.hilt.components.SingletonComponent;
import dagger.multibindings.IntoMap;
import dagger.multibindings.StringKey;
import javax.annotation.processing.Generated;

@Generated("androidx.hilt.AndroidXHiltProcessor")
@Module
@InstallIn(SingletonComponent.class)
@OriginatingElement(
    topLevelClass = LocationCaptureWorker.class
)
public interface LocationCaptureWorker_HiltModule {
  @Binds
  @IntoMap
  @StringKey("com.klarfinance.app.core.location.LocationCaptureWorker")
  WorkerAssistedFactory<? extends ListenableWorker> bind(
      LocationCaptureWorker_AssistedFactory factory);
}
