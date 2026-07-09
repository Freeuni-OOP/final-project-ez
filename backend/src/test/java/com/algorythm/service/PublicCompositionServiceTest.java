package com.algorythm.service;


import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


import com.algorythm.dto.PublicCompositionResponse;
import com.algorythm.model.Composition;
import com.algorythm.model.User;
import com.algorythm.repository.CompositionRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;


/**
 * Unit tests for the public read side of compositions (explore feed + share
 * links). The repository and LikeService are mocked; ordering/filtering against
 * real rows and the actual JSON shape are covered by
 * PublicCompositionControllerIntegrationTest.
 */
@ExtendWith(MockitoExtension.class)
class PublicCompositionServiceTest {


    @Mock private CompositionRepository compositions;
    @Mock private LikeService likeService;


    private PublicCompositionService publicCompositionService;


    private final User alice = new User("alice", "alice@example.com", "hashed-pw");


    @BeforeEach
    void setUp() {
        publicCompositionService = new PublicCompositionService(compositions, likeService);
    }


    // --- feed ------------------------------------------------------------


    @Test
    void feed_usesTheRequestedPageAndSize() {
        when(compositions.findByIsPublicTrueOrderByUpdatedAtDesc(any())).thenReturn(List.of());
        when(likeService.toResponses(List.of(), "alice")).thenReturn(List.of());


        publicCompositionService.feed(2, 10, null, "alice");


        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(compositions).findByIsPublicTrueOrderByUpdatedAtDesc(captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(2);
        assertThat(captor.getValue().getPageSize()).isEqualTo(10);
    }


    @Test
    void feed_clampsANegativePageToZero() {
        when(compositions.findByIsPublicTrueOrderByUpdatedAtDesc(any())).thenReturn(List.of());
        when(likeService.toResponses(any(), any())).thenReturn(List.of());


        publicCompositionService.feed(-5, 10, null, null);


        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(compositions).findByIsPublicTrueOrderByUpdatedAtDesc(captor.capture());
        assertThat(captor.getValue().getPageNumber()).isZero();
    }


    @Test
    void feed_clampsASizeBelowOneUpToOne() {
        when(compositions.findByIsPublicTrueOrderByUpdatedAtDesc(any())).thenReturn(List.of());
        when(likeService.toResponses(any(), any())).thenReturn(List.of());


        publicCompositionService.feed(0, 0, null, null);


        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(compositions).findByIsPublicTrueOrderByUpdatedAtDesc(captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(1);
    }


    @Test
    void feed_clampsAnOversizedPageSizeDownToTheMax() {
        when(compositions.findByIsPublicTrueOrderByUpdatedAtDesc(any())).thenReturn(List.of());
        when(likeService.toResponses(any(), any())).thenReturn(List.of());


        publicCompositionService.feed(0, 1000, null, null);


        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(compositions).findByIsPublicTrueOrderByUpdatedAtDesc(captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(50);
    }


    @Test
    void feed_returnsWhatLikeServiceBuildsForTheAskingViewer() {
        Composition composition = new Composition(alice, "Song", "pattern", 100);
        PublicCompositionResponse response =
                new PublicCompositionResponse(
                        "slug1", "Song", "pattern", 100, "alice", null, null, 3L, true, List.of());
        when(compositions.findByIsPublicTrueOrderByUpdatedAtDesc(any()))
                .thenReturn(List.of(composition));
        when(likeService.toResponses(List.of(composition), "bob")).thenReturn(List.of(response));


        List<PublicCompositionResponse> result = publicCompositionService.feed(0, 20, null, "bob");


        assertThat(result).containsExactly(response);
    }


    @Test
    void feed_filtersByTagWhenGiven() {
        when(compositions.findByIsPublicTrueAndTagName(eq("lofi"), any())).thenReturn(List.of());
        when(likeService.toResponses(any(), any())).thenReturn(List.of());


        publicCompositionService.feed(0, 20, "lofi", null);


        verify(compositions).findByIsPublicTrueAndTagName(eq("lofi"), any());
        verify(compositions, never()).findByIsPublicTrueOrderByUpdatedAtDesc(any());
    }


    @Test
    void feed_normalizesTheTagBeforeFiltering() {
        when(compositions.findByIsPublicTrueAndTagName(eq("lofi"), any())).thenReturn(List.of());
        when(likeService.toResponses(any(), any())).thenReturn(List.of());


        publicCompositionService.feed(0, 20, "  Lofi  ", null);


        verify(compositions).findByIsPublicTrueAndTagName(eq("lofi"), any());
    }


    @Test
    void feed_treatsABlankTagAsNoFilter() {
        when(compositions.findByIsPublicTrueOrderByUpdatedAtDesc(any())).thenReturn(List.of());
        when(likeService.toResponses(any(), any())).thenReturn(List.of());


        publicCompositionService.feed(0, 20, "   ", null);


        verify(compositions, never()).findByIsPublicTrueAndTagName(any(), any());
    }


    // --- search --------------------------------------------------------------


    @Test
    void search_returnsEmptyForABlankQueryWithoutQueryingTheRepository() {
        assertThat(publicCompositionService.search("  ", 0, 20, null)).isEmpty();
        assertThat(publicCompositionService.search(null, 0, 20, null)).isEmpty();
        verify(compositions, never()).searchPublicByTitleOrTag(any(), any());
    }


    @Test
    void search_delegatesToTheRepositoryAndLikeServiceForTheAskingViewer() {
        Composition composition = new Composition(alice, "Midnight Drive", "pattern", 100);
        PublicCompositionResponse response =
                new PublicCompositionResponse(
                        "slug1", "Midnight Drive", "pattern", 100, "alice", null, null, 0L, false,
                        List.of());
        when(compositions.searchPublicByTitleOrTag(eq("midnight"), any()))
                .thenReturn(List.of(composition));
        when(likeService.toResponses(List.of(composition), "bob")).thenReturn(List.of(response));


        List<PublicCompositionResponse> result =
                publicCompositionService.search("midnight", 0, 20, "bob");


        assertThat(result).containsExactly(response);
    }


    // --- getBySlug ---------------------------------------------------------


    @Test
    void getBySlug_returnsThePublishedCompositionForTheAskingViewer() {
        Composition composition = new Composition(alice, "Song", "pattern", 100);
        composition.setPublic(true);
        composition.setSlug("share001");
        PublicCompositionResponse response =
                new PublicCompositionResponse(
                        "share001", "Song", "pattern", 100, "alice", null, null, 0L, false, List.of());
        when(compositions.findBySlugAndIsPublicTrue("share001")).thenReturn(Optional.of(composition));
        when(likeService.toResponse(composition, "bob")).thenReturn(response);


        PublicCompositionResponse result = publicCompositionService.getBySlug("share001", "bob");


        assertThat(result).isEqualTo(response);
        verify(likeService).toResponse(eq(composition), eq("bob"));
    }


    @Test
    void getBySlug_returnsNotFoundWhenTheSlugIsUnknownOrTheCompositionIsntPublic() {
        when(compositions.findBySlugAndIsPublicTrue("missing")).thenReturn(Optional.empty());


        ResponseStatusException ex =
                assertThrows(
                        ResponseStatusException.class,
                        () -> publicCompositionService.getBySlug("missing", null));


        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}



