package omtteam.openmodularturrets.core.storage;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AmmoSortTest {
    private static final int SLOTS = 9;

    /**
     * An item as the sort sees it: registry name, variant name and data. Compared by identity, as the game layers'
     * item stacks are.
     */
    private static final class FakeItem {
        final String id;
        final String variant;
        final String data;
        final int maxStack;

        FakeItem(String id, String variant, String data, int maxStack) {
            this.id = id;
            this.variant = variant;
            this.data = data;
            this.maxStack = maxStack;
        }

        @Override
        public String toString() {
            return id + "/" + variant + (data.isEmpty() ? "" : "{" + data + "}");
        }
    }

    /** Items of {@link FakeItem}s; a slot holds {@code stacks} stacks of an item. */
    private static AmmoSort.Items<FakeItem> items(int stacks) {
        return new AmmoSort.Items<>() {
            @Override
            public boolean isSameKind(FakeItem a, FakeItem b) {
                return a.id.equals(b.id) && a.variant.equals(b.variant) && a.data.equals(b.data);
            }

            @Override
            public String registryName(FakeItem item) {
                return item.id;
            }

            @Override
            public String variantName(FakeItem item) {
                return item.variant;
            }

            @Override
            public int slotLimit(FakeItem item) {
                return item.maxStack * stacks;
            }
        };
    }

    private static FakeItem item(String id) {
        return new FakeItem(id, "item." + id, "", 64);
    }

    private static final FakeItem ARROW = item("minecraft:arrow");
    private static final FakeItem FIRE_CHARGE = item("minecraft:fire_charge");
    private static final FakeItem BOLT = item("openmodularturrets:ammo_bolt");
    private static final FakeItem BULLET = item("openmodularturrets:ammo_bullet");
    private static final FakeItem ROCKET = item("openmodularturrets:ammo_rocket");
    private static final FakeItem OTHER_MOD = item("aaa:shell");

    private static AmmoSort.Stack<FakeItem> stack(FakeItem item, int count) {
        return new AmmoSort.Stack<>(item, count);
    }

    private static List<AmmoSort.Stack<FakeItem>> sorted(int stacks, List<AmmoSort.Stack<FakeItem>> slots) {
        Optional<List<AmmoSort.Stack<FakeItem>>> result = AmmoSort.sort(slots, SLOTS, items(stacks));
        assertTrue(result.isPresent(), "the stacks fit");
        return result.get();
    }

    private static void assertSlots(List<AmmoSort.Stack<FakeItem>> actual, Object... expected) {
        assertEquals(expected.length / 2, actual.size(), "stacks " + actual);
        for (int i = 0; i < actual.size(); i++) {
            assertTrue(actual.get(i).item() == expected[2 * i], "slot " + i + " holds " + actual.get(i));
            assertEquals(expected[2 * i + 1], actual.get(i).count(), "count in slot " + i);
        }
    }

    /** The example of the game test {@code BaseGuiGameTests.sortButtonSortsTheAmmo}. */
    @Test
    void mergesAndSortsMinecraftFirstThenOmtByName() {
        List<AmmoSort.Stack<FakeItem>> result = sorted(1, List.of(stack(ROCKET, 3), stack(ARROW, 40),
                stack(BULLET, 50), stack(FIRE_CHARGE, 5), stack(BULLET, 30), stack(ARROW, 40), stack(BOLT, 7)));
        assertSlots(result, ARROW, 64, ARROW, 16, FIRE_CHARGE, 5, BOLT, 7, BULLET, 64, BULLET, 16, ROCKET, 3);
    }

    @Test
    void otherModsComeLast() {
        List<AmmoSort.Stack<FakeItem>> result = sorted(1, List.of(stack(OTHER_MOD, 1), stack(BULLET, 2),
                stack(ARROW, 3)));
        assertSlots(result, ARROW, 3, BULLET, 2, OTHER_MOD, 1);
    }

    /** Variants that share one registry name are ordered by their variant name. */
    @Test
    void variantsOfOneItemAreOrderedByTheirName() {
        FakeItem rocket = new FakeItem("openmodularturrets:ammo_meta", "item.rocket_craftable", "", 64);
        FakeItem bullet = new FakeItem("openmodularturrets:ammo_meta", "item.bullet_craftable", "", 64);
        List<AmmoSort.Stack<FakeItem>> result = sorted(1, List.of(stack(rocket, 4), stack(bullet, 5)));
        assertSlots(result, bullet, 5, rocket, 4);
    }

    @Test
    void itemsWithOtherDataStaySeparateAndKeepTheirOrder() {
        FakeItem named = new FakeItem("minecraft:arrow", "item.arrow", "name", 64);
        List<AmmoSort.Stack<FakeItem>> result = sorted(1, List.of(stack(named, 10), stack(BULLET, 1),
                stack(ARROW, 20), stack(named, 5)));
        assertSlots(result, named, 15, ARROW, 20, BULLET, 1);
    }

    /** Large-stack slots (base tier stacks, inventory expanders) take more than one stack each. */
    @Test
    void respectsLargeSlotLimits() {
        List<AmmoSort.Stack<FakeItem>> result = sorted(3, List.of(stack(BULLET, 150), stack(ARROW, 1),
                stack(BULLET, 150)));
        assertSlots(result, ARROW, 1, BULLET, 192, BULLET, 108);
    }

    @Test
    void itemsThatStackToOneFillOneSlotEach() {
        FakeItem bow = new FakeItem("minecraft:bow", "item.bow", "", 1);
        List<AmmoSort.Stack<FakeItem>> result = sorted(1, List.of(stack(bow, 1), stack(ARROW, 1), stack(bow, 1)));
        assertSlots(result, ARROW, 1, bow, 1, bow, 1);
    }

    /** Slots above their limit (an expander was removed) that would need more slots: nothing changes. */
    @Test
    void givesUpWhenTheStacksDoNotFit() {
        List<AmmoSort.Stack<FakeItem>> slots = new ArrayList<>();
        for (int i = 0; i < SLOTS; i++) {
            slots.add(stack(BULLET, 100));
        }
        assertFalse(AmmoSort.sort(slots, SLOTS, items(1)).isPresent());
    }

    @Test
    void emptySlotsAreSkipped() {
        assertSlots(sorted(1, List.of(stack(BULLET, 0), stack(ARROW, 2))), ARROW, 2);
        assertTrue(sorted(1, new ArrayList<>()).isEmpty());
        // an empty slot does not decide the order of kinds with the same names
        FakeItem named = new FakeItem(ARROW.id, ARROW.variant, "name", 64);
        assertSlots(sorted(1, List.of(stack(named, 0), stack(ARROW, 20), stack(named, 5))), ARROW, 20, named, 5);
    }

    /** A slot limit below 1 (a faulty game layer) counts as 1 instead of never ending a stack. */
    @Test
    void slotLimitsBelowOneCountAsOne() {
        FakeItem broken = new FakeItem("aaa:broken", "item.broken", "", 0);
        assertSlots(sorted(1, List.of(stack(broken, 2))), broken, 1, broken, 1);
    }

    @Test
    void ranksNamespaces() {
        assertEquals(0, AmmoSort.namespaceRank("minecraft:arrow"));
        assertEquals(0, AmmoSort.namespaceRank("arrow"));
        assertEquals(2, AmmoSort.namespaceRank(":arrow"), "an empty namespace is not Minecraft's");
        assertEquals(1, AmmoSort.namespaceRank("openmodularturrets:ammo_meta"));
        assertEquals(2, AmmoSort.namespaceRank("thermalfoundation:material"));
    }

    /** Random slots: the sort never loses or adds items and never fills a slot above its limit. */
    @Test
    void neverLosesOrDuplicatesItems() {
        FakeItem[] kinds = {ARROW, FIRE_CHARGE, BOLT, BULLET, ROCKET, OTHER_MOD,
                new FakeItem("minecraft:arrow", "item.arrow", "name", 64),
                new FakeItem("minecraft:bow", "item.bow", "", 1),
                new FakeItem("aaa:pearl", "item.pearl", "", 16)};
        Random random = new Random(42);
        for (int run = 0; run < 2000; run++) {
            int stacks = 1 + random.nextInt(4);
            AmmoSort.Items<FakeItem> items = items(stacks);
            List<AmmoSort.Stack<FakeItem>> slots = new ArrayList<>();
            for (int slot = 0; slot < SLOTS; slot++) {
                FakeItem item = kinds[random.nextInt(kinds.length)];
                int count = random.nextInt(4) == 0 ? 0 : 1 + random.nextInt(items.slotLimit(item));
                slots.add(stack(item, count));
            }
            Optional<List<AmmoSort.Stack<FakeItem>>> result = AmmoSort.sort(slots, SLOTS, items);
            // all slots were within their limits, so merging can only need fewer slots
            assertTrue(result.isPresent(), "run " + run);
            assertTrue(result.get().size() <= SLOTS);
            assertEquals(totals(slots), totals(result.get()), "run " + run);
            for (AmmoSort.Stack<FakeItem> stack : result.get()) {
                assertTrue(stack.count() > 0 && stack.count() <= items.slotLimit(stack.item()),
                        "run " + run + ": " + stack);
            }
        }
    }

    private static Map<FakeItem, Integer> totals(List<AmmoSort.Stack<FakeItem>> stacks) {
        Map<FakeItem, Integer> totals = new HashMap<>();
        for (AmmoSort.Stack<FakeItem> stack : stacks) {
            if (stack.count() > 0) totals.merge(stack.item(), stack.count(), Integer::sum);
        }
        return totals;
    }
}
