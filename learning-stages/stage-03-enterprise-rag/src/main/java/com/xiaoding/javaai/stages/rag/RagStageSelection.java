package com.xiaoding.javaai.stages.rag;

import java.util.List;

record RagStageSelection(List<Integer> lessons) {

    private static final List<Integer> ALL_LESSONS = List.of(13, 14, 15, 16, 17, 18, 19, 20, 21);

    static RagStageSelection parse(String[] args) {
        if (args.length == 0 || args.length == 1 && "all".equalsIgnoreCase(args[0])) {
            return new RagStageSelection(ALL_LESSONS);
        }
        if (args.length != 1) {
            throw usageError();
        }
        try {
            int lesson = Integer.parseInt(args[0]);
            if (!ALL_LESSONS.contains(lesson)) throw usageError();
            return new RagStageSelection(List.of(lesson));
        } catch (NumberFormatException error) {
            throw usageError();
        }
    }

    private static IllegalArgumentException usageError() {
        return new IllegalArgumentException("Program arguments 只能填写 13 到 21，或填写 all");
    }
}
