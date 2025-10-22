/*
 * Copyright 2025 Hancom Inc.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.opendataloader.pdf.utils;

import org.verapdf.wcag.algorithms.entities.SemanticTextNode;
import org.verapdf.wcag.algorithms.entities.content.TextChunk;
import org.verapdf.wcag.algorithms.entities.geometry.BoundingBox;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;

/**
 * Tracks inline text decorations that need to be re-applied in downstream generators.
 */
public final class TextDecorations {

    private static final double POSITION_EPSILON = 1.0;
    private static final Queue<TextDecorationMark> UNDERLINE_MARKS = new ArrayDeque<>();
    private static final Set<TextChunk> REGISTERED_CHUNKS =
        Collections.newSetFromMap(new IdentityHashMap<>());

    private TextDecorations() {
    }

    public static void clear() {
        UNDERLINE_MARKS.clear();
        REGISTERED_CHUNKS.clear();
    }

    public static void markUnderline(TextChunk chunk) {
        if (chunk == null) {
            return;
        }
        String value = chunk.getValue();
        if (value == null || value.isBlank()) {
            return;
        }
        if (!REGISTERED_CHUNKS.add(chunk)) {
            return;
        }
        UNDERLINE_MARKS.add(new TextDecorationMark(chunk));
    }

    public static List<TextDecorationMark> pullUnderlinesFor(SemanticTextNode textNode) {
        if (textNode == null || UNDERLINE_MARKS.isEmpty()) {
            return Collections.emptyList();
        }
        BoundingBox nodeBox = textNode.getBoundingBox();
        if (nodeBox == null) {
            return Collections.emptyList();
        }
        Integer pageNumber = textNode.getPageNumber();
        List<TextDecorationMark> matches = new ArrayList<>();
        Iterator<TextDecorationMark> iterator = UNDERLINE_MARKS.iterator();
        while (iterator.hasNext()) {
            TextDecorationMark mark = iterator.next();
            if (mark.belongsTo(pageNumber, nodeBox)) {
                matches.add(mark);
                iterator.remove();
            }
        }
        if (matches.isEmpty()) {
            return Collections.emptyList();
        }
        return matches;
    }

    public static final class TextDecorationMark {
        private final String text;
        private final Integer pageNumber;
        private final double leftX;
        private final double rightX;
        private final double bottomY;
        private final double topY;

        private TextDecorationMark(TextChunk chunk) {
            this.text = chunk.getValue();
            this.pageNumber = chunk.getPageNumber();
            BoundingBox box = chunk.getBoundingBox();
            if (box != null) {
                this.leftX = box.getLeftX();
                this.rightX = box.getRightX();
                this.bottomY = box.getBottomY();
                this.topY = box.getTopY();
            } else {
                this.leftX = 0;
                this.rightX = 0;
                this.bottomY = 0;
                this.topY = 0;
            }
        }

        public String getText() {
            return text;
        }

        public double getLeftX() {
            return leftX;
        }

        private boolean belongsTo(Integer pageNumber, BoundingBox nodeBox) {
            if (!Objects.equals(this.pageNumber, pageNumber)) {
                return false;
            }
            return this.leftX >= nodeBox.getLeftX() - POSITION_EPSILON &&
                this.rightX <= nodeBox.getRightX() + POSITION_EPSILON &&
                this.bottomY >= nodeBox.getBottomY() - POSITION_EPSILON &&
                this.topY <= nodeBox.getTopY() + POSITION_EPSILON;
        }
    }
}
