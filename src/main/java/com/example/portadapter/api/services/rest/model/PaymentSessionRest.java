package com.example.portadapter.api.services.rest.model;

import com.example.portadapter.api.domain.model.PaymentSession;
import com.example.portadapter.api.domain.model.PaymentStatus;
import lombok.Getter;
import lombok.Setter;
import lombok.Value;

@Getter
@Setter
@Value
public class PaymentSessionRest extends PaymentSession<String> {

    private String id;

    private String currency;

    private Double amount;

    private PaymentStatus status;

    private String amountDueGuid;
}
