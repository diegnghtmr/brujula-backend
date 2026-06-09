package co.edu.uniquindio.brujula.offering.application.dto;

import java.util.List;

/**
 * Paged result for listing offerings.
 *
 * @param content the current page of offering summaries
 * @param page zero-based page number
 * @param size page size used
 * @param totalElements total count across all pages
 */
public record OfferingPageView(
    List<OfferingSummaryView> content, int page, int size, long totalElements) {}
