package com.ecommerce.product.service.query.impl;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.ecommerce.product.dto.request.ProductSearchRequest;
import com.ecommerce.product.dto.response.ProductResponse;
import com.ecommerce.product.dto.response.ProductSearchResponse;
import com.ecommerce.product.exception.ProductNotFoundException;
import com.ecommerce.product.mapper.ProductDocumentMapper;
import com.ecommerce.product.mapper.ProductMapper;
import com.ecommerce.product.model.document.ProductDocument;
import com.ecommerce.product.model.entity.Product;
import com.ecommerce.product.repository.read.ProductSearchRepository;
import com.ecommerce.product.repository.write.ProductRepository;
import com.ecommerce.product.service.query.ProductQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional(readOnly = true)
public class ProductQueryServiceImpl implements ProductQueryService {

    private static final Logger log = LoggerFactory.getLogger(ProductQueryServiceImpl.class);
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "price", "name.keyword", "createdAt", "updatedAt", "_score"
    );

    private final ProductSearchRepository searchRepository;
    private final ProductRepository writeRepository;
    private final ElasticsearchOperations esOperations;
    private final ProductDocumentMapper documentMapper;
    private final ProductMapper productMapper;

    public ProductQueryServiceImpl(ProductSearchRepository searchRepository,
                                   ProductRepository writeRepository,
                                   ElasticsearchOperations esOperations,
                                   ProductDocumentMapper documentMapper,
                                   ProductMapper productMapper) {
        this.searchRepository = searchRepository;
        this.writeRepository = writeRepository;
        this.esOperations = esOperations;
        this.documentMapper = documentMapper;
        this.productMapper = productMapper;
    }

    @Override
    public ProductResponse getProductById(UUID id) {
        String idStr = id.toString();

        Optional<ProductDocument> esDoc = searchRepository.findById(idStr);
        if (esDoc.isPresent()) {
            return documentMapper.toResponse(esDoc.get());
        }

        log.debug("Product {} not found in ES; falling back to MySQL", id);
        Product product = writeRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        return productMapper.toResponse(product);
    }

    @Override
    public ProductSearchResponse searchProducts(ProductSearchRequest request) {
        int page = request.page() != null ? Math.max(request.page(), 0) : DEFAULT_PAGE;
        int size = request.size() != null ? Math.min(request.size(), MAX_SIZE) : DEFAULT_SIZE;

        NativeQueryBuilder queryBuilder = NativeQuery.builder()
                .withQuery(buildQuery(request))
                .withPageable(org.springframework.data.domain.PageRequest.of(page, size))
                .withSort(buildSort(request));

        if (request.searchAfter() != null && !request.searchAfter().isBlank()) {
            List<Object> searchAfterValues = decodeSearchAfter(request.searchAfter());
            if (searchAfterValues != null && !searchAfterValues.isEmpty()) {
                queryBuilder.withSearchAfter(searchAfterValues);
            }
        }

        SearchHits<ProductDocument> hits = esOperations.search(
                queryBuilder.build(), ProductDocument.class);

        List<ProductResponse> products = hits.getSearchHits().stream()
                .map(hit -> documentMapper.toResponse(hit.getContent()))
                .toList();

        long totalHits = hits.getTotalHits();
        int totalPages = (int) Math.ceil((double) totalHits / size);
        boolean hasNext = (long) (page + 1) * size < totalHits;

        String nextSearchAfter = null;
        if (hasNext && !hits.getSearchHits().isEmpty()) {
            List<Object> lastSortValues = hits.getSearchHits()
                    .get(hits.getSearchHits().size() - 1).getSortValues();
            if (lastSortValues != null && !lastSortValues.isEmpty()) {
                nextSearchAfter = encodeSearchAfter(lastSortValues);
            }
        }

        return new ProductSearchResponse(
                products, totalHits, page, size, totalPages, hasNext, nextSearchAfter);
    }

    // ── Query Construction ───────────────────────────────────────────

    private Query buildQuery(ProductSearchRequest request) {
        return Query.of(q -> q.bool(b -> {

            if (request.query() != null && !request.query().isBlank()) {
                b.must(m -> m.multiMatch(mm -> mm
                        .fields("name^3", "description", "name.autocomplete^2")
                        .query(request.query())
                        .type(co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType.BestFields)
                        .fuzziness("AUTO")
                        .prefixLength(2)));
            } else {
                b.must(m -> m.matchAll(ma -> ma));
            }

            if (request.category() != null && !request.category().isBlank()) {
                b.filter(f -> f.term(t -> t.field("category").value(request.category())));
            }
            if (request.brand() != null && !request.brand().isBlank()) {
                b.filter(f -> f.term(t -> t.field("brand").value(request.brand())));
            }
            if (request.inStock() != null && request.inStock()) {
                b.filter(f -> f.term(t -> t.field("inStock").value(true)));
            }

            // Fixed RangeQuery using the typed query builder
            if (request.minPrice() != null || request.maxPrice() != null) {
                b.filter(f -> f.range(r -> r
                        .number(n -> {
                            n.field("price");
                            if (request.minPrice() != null) n.gte(request.minPrice().doubleValue());
                            if (request.maxPrice() != null) n.lte(request.maxPrice().doubleValue());
                            return n;
                        })
                ));
            }

            b.filter(f -> f.term(t -> t.field("status").value("ACTIVE")));

            return b;
        }));
    }

    private co.elastic.clients.elasticsearch._types.SortOptions buildSort(ProductSearchRequest request) {
        String requestedSort = request.sortBy() != null ? request.sortBy() : "_score";
        final String effectiveSortBy = ALLOWED_SORT_FIELDS.contains(requestedSort) ? requestedSort : "_score";
        SortOrder order = "ASC".equalsIgnoreCase(request.sortDirection()) ? SortOrder.Asc : SortOrder.Desc;

        if ("_score".equals(effectiveSortBy)) {
            return co.elastic.clients.elasticsearch._types.SortOptions.of(s ->
                    s.score(sc -> sc.order(SortOrder.Desc)));
        }

        return co.elastic.clients.elasticsearch._types.SortOptions.of(s ->
                s.field(f -> f.field(effectiveSortBy).order(order)));
    }

    // ── SearchAfter Cursor Encoding ──────────────────────────────────

    private String encodeSearchAfter(List<Object> sortValues) {
        try {
            return Base64.getEncoder().encodeToString(
                    String.join("|", sortValues.stream().map(Object::toString).toList()).getBytes());
        } catch (Exception e) {
            return null;
        }
    }

    private List<Object> decodeSearchAfter(String encoded) {
        try {
            String decoded = new String(Base64.getDecoder().decode(encoded));
            return new ArrayList<>(Arrays.asList(decoded.split("\\|")));
        } catch (Exception e) {
            log.warn("Failed to decode searchAfter cursor: {}", encoded);
            return Collections.emptyList();
        }
    }
}