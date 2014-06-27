/*
 * Copyright 2014 The Billing Project, LLC
 *
 * The Billing Project licenses this file to you under the Apache License, version 2.0
 * (the "License" + you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.killbill.billing.plugin.moneris;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.payment.api.PluginProperty;
import org.killbill.billing.payment.api.TransactionType;
import org.killbill.billing.payment.plugin.api.PaymentPluginStatus;
import org.killbill.billing.payment.plugin.api.PaymentTransactionInfoPlugin;

import JavaAPI.Receipt;

public class MonerisPaymentTransactionInfoPlugin implements PaymentTransactionInfoPlugin {

    private final UUID kbPaymentId;
    private final UUID kbTransactionPaymentId;
    private final Currency currency;

    private final String isVisaDebit;
    private final String statusMessage;
    private final String statusCode;
    private final String cavvResultCode;
    private final String cvdResultCode;
    private final String avsResultCode;
    private final String recurSuccess;
    private final String ticket;
    private final String timedOut;
    private final String txnNumber;
    private final String cardType;
    private final String transAmount;
    private final String message;
    private final String complete;
    private final String transType;
    private final String transDate;
    private final String transTime;
    private final String authCode;
    private final String iso;
    private final String responseCode;
    private final String referenceNum;
    private final String receiptId;

    public MonerisPaymentTransactionInfoPlugin(final UUID kbPaymentId, final UUID kbTransactionPaymentId,
                                               @Nullable final Currency currency, final Receipt receipt) {
        this(kbPaymentId,
             kbTransactionPaymentId,
             currency,
             receipt.getIsVisaDebit(),
             receipt.getStatusMessage(),
             receipt.getStatusCode(),
             receipt.getCavvResultCode(),
             receipt.getCvdResultCode(),
             receipt.getAvsResultCode(),
             receipt.getRecurSuccess(),
             receipt.getTicket(),
             receipt.getTimedOut(),
             receipt.getTxnNumber(),
             receipt.getCardType(),
             receipt.getTransAmount(),
             receipt.getMessage(),
             receipt.getComplete(),
             receipt.getTransType(),
             receipt.getTransDate(),
             receipt.getTransTime(),
             receipt.getAuthCode(),
             receipt.getISO(),
             receipt.getResponseCode(),
             receipt.getReferenceNum(),
             receipt.getReceiptId());
    }

    public MonerisPaymentTransactionInfoPlugin(final UUID kbPaymentId, final UUID kbTransactionPaymentId, @Nullable final Currency currency,
                                               final String isVisaDebit, final String statusMessage,
                                               final String statusCode, final String cavvResultCode, final String cvdResultCode,
                                               final String avsResultCode, final String recurSuccess, final String ticket,
                                               final String timedOut, final String txnNumber, final String cardType,
                                               final String transAmount, final String message, final String complete,
                                               final String transType, final String transDate, final String transTime,
                                               final String authCode, final String iso, final String responseCode,
                                               final String referenceNum, final String receiptId) {
        this.kbPaymentId = kbPaymentId;
        this.kbTransactionPaymentId = kbTransactionPaymentId;
        this.currency = currency;
        this.isVisaDebit = isVisaDebit;
        this.statusMessage = statusMessage;
        this.statusCode = statusCode;
        this.cavvResultCode = cavvResultCode;
        this.cvdResultCode = cvdResultCode;
        this.avsResultCode = avsResultCode;
        this.recurSuccess = recurSuccess;
        this.ticket = ticket;
        this.timedOut = timedOut;
        this.txnNumber = txnNumber;
        this.cardType = cardType;
        this.transAmount = transAmount;
        this.message = message;
        this.complete = complete;
        this.transType = transType;
        this.transDate = transDate;
        this.transTime = transTime;
        this.authCode = authCode;
        this.iso = iso;
        this.responseCode = responseCode;
        this.referenceNum = referenceNum;
        this.receiptId = receiptId;
    }

    @Override
    public UUID getKbPaymentId() {
        return kbPaymentId;
    }

    @Override
    public UUID getKbTransactionPaymentId() {
        return kbTransactionPaymentId;
    }

    @Override
    public TransactionType getTransactionType() {
        if (getTransType() == null) {
            return null;
        } else {
            // TODO Probably wrong, need to check the actual Strings returned
            return TransactionType.valueOf(getTransType());
        }
    }

    @Override
    public BigDecimal getAmount() {
        if (getTransAmount() == null) {
            return null;
        } else {
            return new BigDecimal(getTransAmount());
        }
    }

    @Override
    public Currency getCurrency() {
        return currency;
    }

    @Override
    public DateTime getCreatedDate() {
        return getEffectiveDate();
    }

    @Override
    public DateTime getEffectiveDate() {
        // TODO Timezone? Assume UTC?
        return DateTime.parse(getTransDate() + 'T' + getTransTime() + 'Z', ISODateTimeFormat.dateTimeNoMillis());
    }

    @Override
    public PaymentPluginStatus getStatus() {
        if (getResponseCode() == null) {
            return PaymentPluginStatus.UNDEFINED;
        }

        final int responseCode;
        try {
            responseCode = Integer.parseInt(getResponseCode());
        } catch (final NumberFormatException e) {
            return PaymentPluginStatus.UNDEFINED;
        }

        if (responseCode >= 0 && responseCode <= 49) {
            // Approved
            return PaymentPluginStatus.PROCESSED;
        } else if (responseCode >= 50 && responseCode <= 999) {
            // Declined
            return PaymentPluginStatus.ERROR;
        } else {
            return PaymentPluginStatus.UNDEFINED;
        }
    }

    @Override
    public String getGatewayError() {
        // The StatusMsg is populated when status_check is set to “true” in the request.
        // TODO Is this the right field to return?
        return getMessage();
    }

    @Override
    public String getGatewayErrorCode() {
        // The StatusCode is populated when status_check is set to “true” in the request.
        // <50: Transaction found
        // >=50: Transaction not found
        return getStatusCode();
    }

    @Override
    public String getFirstPaymentReferenceId() {
        return getAuthCode();
    }

    @Override
    public String getSecondPaymentReferenceId() {
        return getTxnNumber();
    }

    @Override
    public List<PluginProperty> getProperties() {
        final List<PluginProperty> properties = new LinkedList<PluginProperty>();

        // order_id specified in request
        properties.add(new PluginProperty("ReceiptId", getReceiptId(), false));
        // The reference number is an 18 character string that references the terminal used to process the transaction
        // as well as the shift, batch and sequence number, This data is typically used to reference transactions on the
        // host systems and must be displayed on any receipt presented to the customer. This information should be stored
        // by the merchant. The following illustrates the breakdown of this field where "660123450010690030” is the reference
        // number returned in the message, "66012345" is the terminal id, "001" is the shift number, "069" is the batch
        // number and "003" is the transaction number within the batch.
        properties.add(new PluginProperty("ReferenceNum", getReferenceNum(), false));
        // Transaction Response Code
        properties.add(new PluginProperty("ResponseCode", getResponseCode(), false));
        // ISO response code
        properties.add(new PluginProperty("ISO", getIso(), false));
        // Authorization code returned from the issuing institution
        properties.add(new PluginProperty("AuthCode", getAuthCode(), false));
        // Processing host time stamp
        properties.add(new PluginProperty("TransTime", getTransTime(), false));
        // Processing host date stamp
        properties.add(new PluginProperty("TransDate", getTransDate(), false));
        // Type of transaction that was performed
        properties.add(new PluginProperty("TransType", getTransType(), false));
        // Transaction was sent to authorization host and a response was received
        properties.add(new PluginProperty("Complete", getComplete(), false));
        // Response description returned from issuing institution
        properties.add(new PluginProperty("Message", getMessage(), false));
        properties.add(new PluginProperty("TransAmount", getTransAmount(), false));
        // Credit Card Type
        properties.add(new PluginProperty("CardType", getCardType(), false));
        // Gateway Transaction identifier
        properties.add(new PluginProperty("TxnNumber", getTxnNumber(), false));
        // Transaction failed due to a process timing out
        properties.add(new PluginProperty("TimedOut", getTimedOut(), false));
        // reserved
        properties.add(new PluginProperty("Ticket", getTicket(), false));
        // Indicates whether the transaction successfully registered
        properties.add(new PluginProperty("RecurSuccess", getRecurSuccess(), false));
        // Indicates the address verification result
        properties.add(new PluginProperty("AvsResultCode", getAvsResultCode(), false));
        // Indicates the CVD validation result
        properties.add(new PluginProperty("CvdResultCode", getCvdResultCode(), false));
        // The CAVV result code indicates the result of the CAVV validation.
        // 0 = CAVV authentication results invalid
        // 1 = CAVV failed validation; authentication
        // 2 = CAVV passed validation; authentication
        // 3 = CAVV passed validation; attempt
        // 4 = CAVV failed validation; attempt
        // 7 = CAVV failed validation; attempt (US issued cards only)
        // 8 = CAVV passed validation; attempt (US issued cards only)
        // 9 = CAVV failed validation; attempt (US issued cards only)
        // A = CAVV passed validation; attempt (US issued cards only)
        // B = CAVV passed validation but downgraded; treat this transaction same as ECI 7
        properties.add(new PluginProperty("CavvResultCode", getCavvResultCode(), false));
        // The StatusCode is populated when status_check is set to “true” in the request
        properties.add(new PluginProperty("StatusCode", getStatusCode(), false));
        // The StatusCode is populated when status_check is set to “true” in the request
        properties.add(new PluginProperty("StatusMsg", getStatusMessage(), false));
        // Indicates whether the card that the transaction was performed on is Visa debit.
        // true = Card is Visa Debit
        // false = Card is not Visa Debit
        // null = there was an error in identifying the card
        properties.add(new PluginProperty("IsVisaDebit", getIsVisaDebit(), false));

        return properties;
    }

    public String getIsVisaDebit() {
        return isVisaDebit;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public String getStatusCode() {
        return statusCode;
    }

    public String getCavvResultCode() {
        return cavvResultCode;
    }

    public String getCvdResultCode() {
        return cvdResultCode;
    }

    public String getAvsResultCode() {
        return avsResultCode;
    }

    public String getRecurSuccess() {
        return recurSuccess;
    }

    public String getTicket() {
        return ticket;
    }

    public String getTimedOut() {
        return timedOut;
    }

    public String getTxnNumber() {
        return txnNumber;
    }

    public String getCardType() {
        return cardType;
    }

    public String getTransAmount() {
        return transAmount;
    }

    public String getMessage() {
        return message;
    }

    public String getComplete() {
        return complete;
    }

    public String getTransType() {
        return transType;
    }

    public String getTransDate() {
        return transDate;
    }

    public String getTransTime() {
        return transTime;
    }

    public String getAuthCode() {
        return authCode;
    }

    public String getIso() {
        return iso;
    }

    public String getResponseCode() {
        return responseCode;
    }

    public String getReferenceNum() {
        return referenceNum;
    }

    public String getReceiptId() {
        return receiptId;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("MonerisPaymentTransactionInfoPlugin{");
        sb.append("kbPaymentId=").append(kbPaymentId);
        sb.append(", kbTransactionPaymentId=").append(kbTransactionPaymentId);
        sb.append(", currency=").append(currency);
        sb.append(", isVisaDebit='").append(isVisaDebit).append('\'');
        sb.append(", statusMessage='").append(statusMessage).append('\'');
        sb.append(", statusCode='").append(statusCode).append('\'');
        sb.append(", cavvResultCode='").append(cavvResultCode).append('\'');
        sb.append(", cvdResultCode='").append(cvdResultCode).append('\'');
        sb.append(", avsResultCode='").append(avsResultCode).append('\'');
        sb.append(", recurSuccess='").append(recurSuccess).append('\'');
        sb.append(", ticket='").append(ticket).append('\'');
        sb.append(", timedOut='").append(timedOut).append('\'');
        sb.append(", txnNumber='").append(txnNumber).append('\'');
        sb.append(", cardType='").append(cardType).append('\'');
        sb.append(", transAmount='").append(transAmount).append('\'');
        sb.append(", message='").append(message).append('\'');
        sb.append(", complete='").append(complete).append('\'');
        sb.append(", transType='").append(transType).append('\'');
        sb.append(", transDate='").append(transDate).append('\'');
        sb.append(", transTime='").append(transTime).append('\'');
        sb.append(", authCode='").append(authCode).append('\'');
        sb.append(", iso='").append(iso).append('\'');
        sb.append(", responseCode='").append(responseCode).append('\'');
        sb.append(", referenceNum='").append(referenceNum).append('\'');
        sb.append(", receiptId='").append(receiptId).append('\'');
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final MonerisPaymentTransactionInfoPlugin that = (MonerisPaymentTransactionInfoPlugin) o;

        if (authCode != null ? !authCode.equals(that.authCode) : that.authCode != null) {
            return false;
        }
        if (avsResultCode != null ? !avsResultCode.equals(that.avsResultCode) : that.avsResultCode != null) {
            return false;
        }
        if (cardType != null ? !cardType.equals(that.cardType) : that.cardType != null) {
            return false;
        }
        if (cavvResultCode != null ? !cavvResultCode.equals(that.cavvResultCode) : that.cavvResultCode != null) {
            return false;
        }
        if (complete != null ? !complete.equals(that.complete) : that.complete != null) {
            return false;
        }
        if (currency != that.currency) {
            return false;
        }
        if (cvdResultCode != null ? !cvdResultCode.equals(that.cvdResultCode) : that.cvdResultCode != null) {
            return false;
        }
        if (isVisaDebit != null ? !isVisaDebit.equals(that.isVisaDebit) : that.isVisaDebit != null) {
            return false;
        }
        if (iso != null ? !iso.equals(that.iso) : that.iso != null) {
            return false;
        }
        if (kbPaymentId != null ? !kbPaymentId.equals(that.kbPaymentId) : that.kbPaymentId != null) {
            return false;
        }
        if (kbTransactionPaymentId != null ? !kbTransactionPaymentId.equals(that.kbTransactionPaymentId) : that.kbTransactionPaymentId != null) {
            return false;
        }
        if (message != null ? !message.equals(that.message) : that.message != null) {
            return false;
        }
        if (receiptId != null ? !receiptId.equals(that.receiptId) : that.receiptId != null) {
            return false;
        }
        if (recurSuccess != null ? !recurSuccess.equals(that.recurSuccess) : that.recurSuccess != null) {
            return false;
        }
        if (referenceNum != null ? !referenceNum.equals(that.referenceNum) : that.referenceNum != null) {
            return false;
        }
        if (responseCode != null ? !responseCode.equals(that.responseCode) : that.responseCode != null) {
            return false;
        }
        if (statusCode != null ? !statusCode.equals(that.statusCode) : that.statusCode != null) {
            return false;
        }
        if (statusMessage != null ? !statusMessage.equals(that.statusMessage) : that.statusMessage != null) {
            return false;
        }
        if (ticket != null ? !ticket.equals(that.ticket) : that.ticket != null) {
            return false;
        }
        if (timedOut != null ? !timedOut.equals(that.timedOut) : that.timedOut != null) {
            return false;
        }
        if (transAmount != null ? !transAmount.equals(that.transAmount) : that.transAmount != null) {
            return false;
        }
        if (transDate != null ? !transDate.equals(that.transDate) : that.transDate != null) {
            return false;
        }
        if (transTime != null ? !transTime.equals(that.transTime) : that.transTime != null) {
            return false;
        }
        if (transType != null ? !transType.equals(that.transType) : that.transType != null) {
            return false;
        }
        if (txnNumber != null ? !txnNumber.equals(that.txnNumber) : that.txnNumber != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = kbPaymentId != null ? kbPaymentId.hashCode() : 0;
        result = 31 * result + (kbTransactionPaymentId != null ? kbTransactionPaymentId.hashCode() : 0);
        result = 31 * result + (currency != null ? currency.hashCode() : 0);
        result = 31 * result + (isVisaDebit != null ? isVisaDebit.hashCode() : 0);
        result = 31 * result + (statusMessage != null ? statusMessage.hashCode() : 0);
        result = 31 * result + (statusCode != null ? statusCode.hashCode() : 0);
        result = 31 * result + (cavvResultCode != null ? cavvResultCode.hashCode() : 0);
        result = 31 * result + (cvdResultCode != null ? cvdResultCode.hashCode() : 0);
        result = 31 * result + (avsResultCode != null ? avsResultCode.hashCode() : 0);
        result = 31 * result + (recurSuccess != null ? recurSuccess.hashCode() : 0);
        result = 31 * result + (ticket != null ? ticket.hashCode() : 0);
        result = 31 * result + (timedOut != null ? timedOut.hashCode() : 0);
        result = 31 * result + (txnNumber != null ? txnNumber.hashCode() : 0);
        result = 31 * result + (cardType != null ? cardType.hashCode() : 0);
        result = 31 * result + (transAmount != null ? transAmount.hashCode() : 0);
        result = 31 * result + (message != null ? message.hashCode() : 0);
        result = 31 * result + (complete != null ? complete.hashCode() : 0);
        result = 31 * result + (transType != null ? transType.hashCode() : 0);
        result = 31 * result + (transDate != null ? transDate.hashCode() : 0);
        result = 31 * result + (transTime != null ? transTime.hashCode() : 0);
        result = 31 * result + (authCode != null ? authCode.hashCode() : 0);
        result = 31 * result + (iso != null ? iso.hashCode() : 0);
        result = 31 * result + (responseCode != null ? responseCode.hashCode() : 0);
        result = 31 * result + (referenceNum != null ? referenceNum.hashCode() : 0);
        result = 31 * result + (receiptId != null ? receiptId.hashCode() : 0);
        return result;
    }
}
