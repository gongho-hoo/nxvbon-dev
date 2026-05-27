package com.nxvibeon.backend.prompt;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nxvibeon.prompt")
public class PromptProperties {
    private String generalChat = "prompts/general-chat-prompt-ko.md";
    private String projectChat = "prompts/project-chat-prompt-ko.md";

    public String getGeneralChat() {
        return generalChat;
    }

    public void setGeneralChat(String generalChat) {
        this.generalChat = generalChat;
    }

    public String getProjectChat() {
        return projectChat;
    }

    public void setProjectChat(String projectChat) {
        this.projectChat = projectChat;
    }
}
