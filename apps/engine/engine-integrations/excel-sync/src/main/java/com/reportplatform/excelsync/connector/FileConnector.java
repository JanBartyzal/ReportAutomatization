package com.reportplatform.excelsync.connector;

import com.reportplatform.excelsync.model.entity.ExportFlowDefinitionEntity;

public interface FileConnector {

    byte[] download(ExportFlowDefinitionEntity flow, String fileName);

    void upload(ExportFlowDefinitionEntity flow, byte[] content, String fileName);

    boolean testConnection(ExportFlowDefinitionEntity flow);
}
