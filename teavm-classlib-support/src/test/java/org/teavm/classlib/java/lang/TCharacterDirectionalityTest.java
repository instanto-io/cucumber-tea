/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.teavm.classlib.java.lang;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;

/**
 * Checks the derived bidi classes against values taken from the JDK.
 *
 * <p>Runs under TeaVM because {@code getType} is backed by a native resource. {@code Character}
 * resolves to the class in this module, so these calls exercise the shim.
 */
@RunWith(TeaVMTestRunner.class)
@SkipJVM
public class TCharacterDirectionalityTest {

  @Test
  public void matchesTheJdkForRepresentativeCharacters() {
    assertDirection('A', Character.DIRECTIONALITY_LEFT_TO_RIGHT);
    assertDirection('5', Character.DIRECTIONALITY_EUROPEAN_NUMBER);
    assertDirection(' ', Character.DIRECTIONALITY_WHITESPACE);
    assertDirection('\n', Character.DIRECTIONALITY_PARAGRAPH_SEPARATOR);
    assertDirection('\t', Character.DIRECTIONALITY_SEGMENT_SEPARATOR);
    assertDirection(',', Character.DIRECTIONALITY_COMMON_NUMBER_SEPARATOR);
    assertDirection('+', Character.DIRECTIONALITY_EUROPEAN_NUMBER_SEPARATOR);
    assertDirection('$', Character.DIRECTIONALITY_EUROPEAN_NUMBER_TERMINATOR);
    assertDirection(0x0301, Character.DIRECTIONALITY_NONSPACING_MARK);
    assertDirection(0x202A, Character.DIRECTIONALITY_LEFT_TO_RIGHT_EMBEDDING);
    assertDirection(0x202E, Character.DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE);
  }

  @Test
  public void separatesRightToLeftScriptsFromArabic() {
    assertDirection(0x05D0, Character.DIRECTIONALITY_RIGHT_TO_LEFT);
    assertDirection(0x05EA, Character.DIRECTIONALITY_RIGHT_TO_LEFT);
    assertDirection(0x07C0, Character.DIRECTIONALITY_RIGHT_TO_LEFT);
    assertDirection(0xFB1F, Character.DIRECTIONALITY_RIGHT_TO_LEFT);
    assertDirection(0x0627, Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC);
    assertDirection(0x0710, Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC);
    assertDirection(0xFB50, Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC);
    assertDirection(0xFE70, Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC);
  }

  @Test
  public void classifiesArabicDigits() {
    assertDirection(0x0660, Character.DIRECTIONALITY_ARABIC_NUMBER);
    assertDirection(0x06F5, Character.DIRECTIONALITY_EUROPEAN_NUMBER);
  }

  private static void assertDirection(int codePoint, byte expected) {
    assertEquals(
        "codePoint " + Integer.toHexString(codePoint),
        expected,
        Character.getDirectionality(codePoint));
  }
}
