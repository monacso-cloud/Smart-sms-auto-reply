package com.smartreply.beta;

import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import static org.junit.Assert.*;

public class ChatbotRegressionTest {
    private static String rules;
    @BeforeClass public static void loadActualTemplates() throws Exception {
        NodeList strings = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new File("src/main/res/values/strings.xml")).getElementsByTagName("string");
        for (int i = 0; i < strings.getLength(); i++) {
            if ("menu_chatbot_rules".equals(strings.item(i).getAttributes().getNamedItem("name").getNodeValue())) {
                rules = strings.item(i).getTextContent().replace("\\n", "\n");
            }
        }
        assertNotNull(rules);
    }
    private String response(String input) { return ChatbotRules.findReply(input, rules); }

    @Test public void everyMenuNumberRoutesToItsOwnReply() {
        assertTrue(response("1").contains("current prices"));
        assertTrue(response("2").contains("current availability"));
        assertTrue(response("3").startsWith("Book your appointment"));
        assertTrue(response("4").contains("personal assistance"));
        assertTrue(response("5").startsWith("To cancel"));
        assertTrue(response("6").startsWith("To reschedule"));
    }
    @Test public void rapidMenuSequenceIncludingRepeatedOneIsNotSilenced() {
        ChatbotReplyGate gate = new ChatbotReplyGate("");
        String[] choices = {"1", "2", "1", "3", "4", "5", "6"};
        long now = 100_000L;
        // The previous welcome/fallback must not suppress the customer's menu choice.
        gate.blockedReason("welcome", false, now);
        gate.recordAccepted("welcome", false, now);
        for (int i = 0; i < choices.length; i++) {
            assertNotNull(response(choices[i]));
            assertNull(gate.blockedReason("pdu-" + i, true, now + i + 1));
            gate.recordAccepted("pdu-" + i, true, now + i + 1);
            gate = new ChatbotReplyGate(gate.save());
        }
    }
    @Test public void duplicateBroadcastDoesNotSendTwiceEvenAfterAnotherChoice() {
        ChatbotReplyGate gate = new ChatbotReplyGate("");
        assertNull(gate.blockedReason("first-pdu", true, 100_000L));
        gate.recordAccepted("first-pdu", true, 100_000L);
        assertNull(gate.blockedReason("second-pdu", true, 100_001L));
        gate.recordAccepted("second-pdu", true, 100_001L);
        assertNotNull(new ChatbotReplyGate(gate.save()).blockedReason("first-pdu", true, 100_002L));
    }
    @Test public void freshSmsWithSameTextCanRetryAfterSynchronousFailure() {
        ChatbotReplyGate gate = new ChatbotReplyGate("");
        assertNull(gate.blockedReason("failed-pdu", true, 100_000L));
        // No recordAccepted if sending throws.
        assertNull(new ChatbotReplyGate(gate.save()).blockedReason("retry-pdu", true, 100_001L));
    }
    @Test public void fallbackLoopAndFloodProtectionRemain() {
        ChatbotReplyGate gate = new ChatbotReplyGate("");
        assertNull(gate.blockedReason("a", false, 100_000L));
        gate.recordAccepted("a", false, 100_000L);
        assertNotNull(gate.blockedReason("b", false, 100_001L));
        assertNull(gate.blockedReason("c", true, 100_002L));
        assertNull(gate.blockedReason("d", false, 130_000L));
        for (int i = 0; i < 19; i++) {
            assertNull(gate.blockedReason("fast-" + i, true, 131_000L + i));
            gate.recordAccepted("fast-" + i, true, 131_000L + i);
        }
        assertNotNull(gate.blockedReason("too-many", true, 132_000L));
        assertNull(gate.blockedReason("next-window", true, 160_000L));
    }
    @Test public void cancellationDoesNotRouteToAvailabilityOrBooking() {
        for (String message : new String[]{"cancel", "Please cancel my appointment", "cancel my booking",
                "Cancellation request", "ยกเลิกนัด"}) {
            assertEquals(message, response("5"), response(message));
        }
        assertEquals(response("2"), response("Any cancellations today?"));
        assertEquals(response("2"), response("cancellation today"));
    }
    @Test public void reschedulingDoesNotRouteToBooking() {
        for (String message : new String[]{"reschedule", "Please reschedule my appointment", "change my appointment",
                "move my appointment", "postpone", "เลื่อนนัด"}) {
            assertEquals(message, response("6"), response(message));
        }
    }
    @Test public void bookingChangesUseConfirmationEmailAndPolicyWindow() {
        for (String choice : new String[]{"5", "6"}) {
            String text = response(choice);
            assertTrue(text.contains("[enter website URL]"));
            assertTrue(text.contains("booking confirmation email"));
            assertTrue(text.contains("notice period"));
            assertTrue(text.contains("A fee may apply"));
            assertTrue(text.contains("confirmed only when"));
        }
    }
    @Test public void pricesAndHumanKeywordsWork() {
        assertEquals(response("1"), response("Prices please"));
        assertEquals(response("4"), response("HUMAN"));
        assertEquals(response("4"), response("Can someone call me?"));
    }
    @Test public void numbersInsidePhoneNumbersAndTimesDoNotSelectOne() {
        assertNull(response("0412345678"));
        assertNull(response("14"));
        assertNull(response("at 4 pm"));
        assertNull(response("4.30"));
    }
    @Test public void whitespaceAndFullWidthMenuDigitsAreAccepted() {
        assertEquals(response("1"), response(" \n1\t"));
        assertEquals(response("4"), response("\u00a0４\u00a0"));
    }
    @Test public void malformedAndEmptyRulesDoNotHideLaterValidRule() {
        assertEquals("working", ChatbotRules.findReply("1", "bad line\n1=>\n1=>working"));
        assertNull(ChatbotRules.findReply("", rules));
        assertNull(ChatbotRules.findReply("1", null));
    }
    @Test public void templateContainsNoOwnerDetailsOrInventedNoticePeriod() throws Exception {
        String text = new String(Files.readAllBytes(new File("src/main/res/values/strings.xml").toPath()), StandardCharsets.UTF_8).toLowerCase();
        for (String privateDetail : new String[]{"ying thai", "aaitacademy", "clinicsense", "piara waters", "broadway", "mona.cso", "+61", "24 hours notice", "48 hours notice"}) {
            assertFalse(privateDetail, text.contains(privateDetail));
        }
    }
    @Test public void storedCustomRepliesStillWinWithoutLoadingTemplate() {
        assertEquals("My existing price reply", ChatbotRules.findReply("1", "1,price=>My existing price reply\n4,human=>My existing help"));
    }
    @Test public void malformedGateStateRecovers() {
        assertNull(new ChatbotReplyGate("bad").blockedReason("new", true, 100_000L));
    }
}
