// src/main/java/com/shirtshop/dto/PageResponse.java
package com.shirtshop.dto;

import java.util.List;

public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {}
