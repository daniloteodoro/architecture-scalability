package com.scale.management.domain;

import static com.scale.management.application.usecases.AddSampleShoppingCarts.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class AddShoppingCartCommandTest {

    @Test
    public void testBuilder() {
        var command = AddShoppingCartCommand.builder()
                .number(10)
                .maxNumberOfClients(10)
                .ackFirst(true)
                .ackLast(true)
                .build();
        assertThat(command.getNumber(), is(equalTo(10L)));
        assertThat(command.getSession(), is(notNullValue()));

        var another = AddShoppingCartCommand.builder()
                .number(10)
                .maxNumberOfClients(10)
                .ackFirst(true)
                .ackLast(true)
                .build();
        assertEquals(command, another);

        var different = AddShoppingCartCommand.builder()
                .number(10)
                .maxNumberOfClients(10)
                .ackFirst(true)
                .ackLast(false)
                .build();
        assertNotEquals(command, different);
    }

    @Test
    public void testSession() {
        assertThrows(NullPointerException.class, () -> AddShoppingCartCommand.builder()
                .number(10)
                .maxNumberOfClients(10)
                .ackFirst(true)
                .ackLast(true)
                .session(null)
                .build());
    }
}
