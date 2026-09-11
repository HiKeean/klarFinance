package com.api.klarfinance.global;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.ImplicitEntityNameSource;
import org.springframework.boot.orm.jpa.hibernate.SpringImplicitNamingStrategy;

public class CustomImplicitNamingStrategy extends SpringImplicitNamingStrategy {

    @Override
    public Identifier determinePrimaryTableName(ImplicitEntityNameSource source) {
        String fullClassName = source.getEntityNaming().getClassName();

        if (fullClassName != null && fullClassName.contains(".")) {
            String[] parts = fullClassName.split("\\.");
            
            String className = parts[parts.length - 1]; 
            String folderName = parts[parts.length - 2]; 

            String logicalName = folderName + "_" + className;
            return toIdentifier(logicalName, source.getBuildingContext());
        }

        return super.determinePrimaryTableName(source);
    }
}