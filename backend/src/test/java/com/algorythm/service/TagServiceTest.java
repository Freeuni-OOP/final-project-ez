package com.algorythm.service;


import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


import com.algorythm.model.Tag;
import com.algorythm.repository.CompositionRepository;
import com.algorythm.repository.TagRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;


/**
 * Unit tests for tag normalization and get-or-create resolution. The
 * repositories are mocked; real dedup/uniqueness against the DB is covered by
 * CompositionServiceIntegrationTest.
 */
@ExtendWith(MockitoExtension.class)
class TagServiceTest {


    @Mock private TagRepository tags;
    @Mock private CompositionRepository compositions;


    private TagService tagService;


    @BeforeEach
    void setUp() {
        tagService = new TagService(tags, compositions);
    }


    // --- resolve -----------------------------------------------------------


    @Test
    void resolve_reusesAnExistingTagInsteadOfCreatingANewOne() {
        Tag existing = new Tag("lofi");
        when(tags.findByName("lofi")).thenReturn(Optional.of(existing));


        Set<Tag> resolved = tagService.resolve(List.of("lofi"));


        assertThat(resolved).containsExactly(existing);
        verify(tags, never()).save(any());
    }


    @Test
    void resolve_createsANewTagWhenItDoesntExistYet() {
        when(tags.findByName("chill")).thenReturn(Optional.empty());
        when(tags.save(any(Tag.class))).thenAnswer(inv -> inv.getArgument(0));


        Set<Tag> resolved = tagService.resolve(List.of("chill"));


        assertThat(resolved).extracting(Tag::getName).containsExactly("chill");
    }


    @Test
    void resolve_normalizesCasingAndWhitespaceSoTheyShareOneTag() {
        Tag existing = new Tag("lofi");
        when(tags.findByName("lofi")).thenReturn(Optional.of(existing));


        Set<Tag> resolved = tagService.resolve(List.of("  Lofi  "));


        assertThat(resolved).containsExactly(existing);
    }


    @Test
    void resolve_deduplicatesNamesThatNormalizeToTheSameTag() {
        Tag existing = new Tag("lofi");
        when(tags.findByName("lofi")).thenReturn(Optional.of(existing));


        Set<Tag> resolved = tagService.resolve(List.of("lofi", "Lofi", " LOFI "));


        assertThat(resolved).hasSize(1);
    }


    @Test
    void resolve_ignoresBlankEntries() {
        Set<Tag> resolved = tagService.resolve(List.of("", "   "));


        assertThat(resolved).isEmpty();
        verify(tags, never()).save(any());
    }


    @Test
    void resolve_returnsEmptyForANullList() {
        assertThat(tagService.resolve(null)).isEmpty();
    }


    @Test
    void resolve_rejectsMoreThanFiveDistinctTags() {
        ResponseStatusException ex =
                assertThrows(
                        ResponseStatusException.class,
                        () -> tagService.resolve(
                                List.of("a", "b", "c", "d", "e", "f")));


        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(tags, never()).save(any());
    }


    @Test
    void resolve_rejectsATagLongerThanThirtyCharacters() {
        String tooLong = "a".repeat(31);


        ResponseStatusException ex =
                assertThrows(
                        ResponseStatusException.class, () -> tagService.resolve(List.of(tooLong)));


        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }


    @Test
    void resolve_fallsBackToTheWinnerWhenTwoRequestsRaceToCreateTheSameTag() {
        Tag winner = new Tag("chill");
        when(tags.findByName("chill"))
                .thenReturn(Optional.empty()) // this request's first look
                .thenReturn(Optional.of(winner)); // after losing the insert race
        when(tags.save(any(Tag.class))).thenThrow(new DataIntegrityViolationException("dup key"));


        Set<Tag> resolved = tagService.resolve(List.of("chill"));


        assertThat(resolved).containsExactly(winner);
    }


    // --- normalize -----------------------------------------------------------


    @Test
    void normalize_lowercasesTrimsAndCollapsesWhitespace() {
        assertThat(TagService.normalize("  Lo-Fi   Beats ")).isEqualTo("lo-fi beats");
    }


    @Test
    void normalize_returnsEmptyForNull() {
        assertThat(TagService.normalize(null)).isEmpty();
    }


    // --- publicTagNames --------------------------------------------------


    @Test
    void publicTagNames_delegatesToTheRepository() {
        when(compositions.findDistinctTagNamesForPublicCompositions())
                .thenReturn(List.of("chill", "lofi"));


        assertThat(tagService.publicTagNames()).containsExactly("chill", "lofi");
    }
}



