package com.xiaoding.javaai.stages.rag;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RagStageSelectionTest {

    @Test
    void defaultsToTheCompleteJourney() {
        assertEquals(List.of(13, 14, 15, 16, 17, 18, 19, 20, 21), RagStageSelection.parse(new String[0]).lessons());
    }

    @Test
    void selectsOneArticleByItsProgramArgument() {
        assertEquals(List.of(17), RagStageSelection.parse(new String[]{"17"}).lessons());
        assertEquals(List.of(13, 14, 15, 16, 17, 18, 19, 20, 21), RagStageSelection.parse(new String[]{"all"}).lessons());
    }

    @Test
    void rejectsArgumentsOutsideTheRagArticles() {
        assertThrows(IllegalArgumentException.class, () -> RagStageSelection.parse(new String[]{"12"}));
        assertThrows(IllegalArgumentException.class, () -> RagStageSelection.parse(new String[]{"13", "14"}));
    }
}
