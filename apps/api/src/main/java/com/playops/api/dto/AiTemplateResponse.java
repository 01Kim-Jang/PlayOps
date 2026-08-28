package com.playops.api.dto;

import java.util.List;

public class AiTemplateResponse {
    private String templateName;
    private String description;
    private List<TemplateFileDto> files;

    public static class TemplateFileDto {
        private String filename;
        private String content;

        public TemplateFileDto() {}
        public TemplateFileDto(String filename, String content) {
            this.filename = filename;
            this.content = content;
        }

        public String getFilename() { return filename; }
        public void setFilename(String filename) { this.filename = filename; }

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }

    public String getTemplateName() { return templateName; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<TemplateFileDto> getFiles() { return files; }
    public void setFiles(List<TemplateFileDto> files) { this.files = files; }
}
