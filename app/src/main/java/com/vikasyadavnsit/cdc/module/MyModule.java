package com.vikasyadavnsit.cdc.module;


import com.vikasyadavnsit.cdc.permissions.PermissionHandler;
import com.vikasyadavnsit.cdc.permissions.PermissionManager;
import com.vikasyadavnsit.cdc.repositories.MyRepository;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

@InstallIn(SingletonComponent.class)
@Module
public class MyModule {
    @Provides
    public MyRepository provideSomeDependency() {
        return new MyRepository();
    }

    @Provides
    public PermissionHandler providePermissionHandler() {
        return new PermissionManager();
    }

}

