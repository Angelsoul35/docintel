package com.angelo.docintel.web.dto;

/** Request body for {@code POST /documents}. {@code id} is optional and generated when blank. */
public record DocumentRequest(String id, String title, String text) {
}
