package com.algorythm;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.algorythm.model.Composition;
import com.algorythm.model.Tag;
import com.algorythm.model.User;
import com.algorythm.repository.CompositionRepository;
import com.algorythm.repository.TagRepository;
import com.algorythm.repository.UserRepository;
import com.algorythm.support.AbstractIntegrationTest;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * End-to-end coverage of the public tag list (backs the explore side's tag
 * filter UI) against a real Postgres, running through the real controller and
 * service. @Transactional rolls each test's writes back.
 */
@AutoConfigureMockMvc
@Transactional
class PublicTagControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private CompositionRepository compositionRepository;
    @Autowired private TagRepository tagRepository;

    private User alice;

    @BeforeEach
    void setUp() {
        alice = userRepository.save(new User("alice", "alice@example.com", "hashed-pw"));
    }

    @Test
    void list_returnsOnlyTagsUsedByAtLeastOnePublishedComposition() throws Exception {
        Tag lofi = tagRepository.save(new Tag("lofi"));
        Tag draftOnly = tagRepository.save(new Tag("draft-only"));

        Composition published = new Composition(alice, "Song", "pattern", 100);
        published.setSlug("tagslist1");
        published.setPublic(true);
        published.setTags(Set.of(lofi));
        compositionRepository.save(published);

        Composition draft = new Composition(alice, "Draft", "pattern", 100);
        draft.setTags(Set.of(draftOnly));
        compositionRepository.save(draft);

        mockMvc.perform(get("/api/public/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0]").value("lofi"));
    }

    @Test
    void list_isEmptyWhenNoPublishedCompositionHasAnyTags() throws Exception {
        mockMvc.perform(get("/api/public/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}