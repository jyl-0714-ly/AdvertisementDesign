package com.advertisementdesign.back.communication.service;

/**
 * Communication-module callback used only for authenticated human designer sends.
 * Generated greetings bypass this boundary and therefore never acknowledge a match.
 */
public interface DesignerMessageAcknowledgementPort {

    void acknowledgeHumanDesignerMessage(Long consultantIntakeId, Long designerId);
}
