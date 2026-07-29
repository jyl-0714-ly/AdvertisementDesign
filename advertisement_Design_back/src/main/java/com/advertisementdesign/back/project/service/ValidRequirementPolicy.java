package com.advertisementdesign.back.project.service;

public interface ValidRequirementPolicy {
    Decision evaluate(String content, boolean hasAttachments);

    record Decision(boolean valid, String guidance) {
    }
}
