package com.reportplatform.excelsync.service;

import com.reportplatform.excelsync.connector.FileConnector;
import com.reportplatform.excelsync.connector.LocalPathWriter;
import com.reportplatform.excelsync.connector.SharePointConnector;
import com.reportplatform.excelsync.model.entity.TargetType;
import org.springframework.stereotype.Component;

@Component
public class FileConnectorFactory {

    private final LocalPathWriter localPathWriter;
    private final SharePointConnector sharePointConnector;

    public FileConnectorFactory(LocalPathWriter localPathWriter,
                                SharePointConnector sharePointConnector) {
        this.localPathWriter = localPathWriter;
        this.sharePointConnector = sharePointConnector;
    }

    public FileConnector getConnector(TargetType targetType) {
        return switch (targetType) {
            case LOCAL_PATH -> localPathWriter;
            case SHAREPOINT -> sharePointConnector;
        };
    }
}
