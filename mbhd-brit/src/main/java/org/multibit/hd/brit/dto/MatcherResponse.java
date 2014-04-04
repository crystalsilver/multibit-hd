package org.multibit.hd.brit.dto;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import org.multibit.hd.brit.exceptions.MatcherResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.Strings;

import java.util.Date;
import java.util.List;

/**
 * <p>DTO to provide the following to BRIT API:</p>
 * <ul>
 * <li>The response message from the Matcher to the Payer</li>
 * </ul>
 *
 * @since 0.0.1
 */
public class MatcherResponse {

  private static final Logger log = LoggerFactory.getLogger(PayerRequest.class);

  public static final String OPTIONAL_NOT_PRESENT_TEXT = "not-present";

  private final Optional<Date> replayDateOptional;

  private final List<String> addressList;

  public MatcherResponse(Optional<Date> replayDateOptional, List<String> addressList) {
    this.replayDateOptional = replayDateOptional;
    this.addressList = addressList;
  }

  public int getVersion() {
    return 1;
  }

  public List<String> getAddressList() {
    return addressList;
  }

  public Optional<Date> getReplayDate() {
    return replayDateOptional;
  }

  /**
   * Serialise a MatcherResponse
   * Format is:
   * versionNumber
   * replayDate
   * address1
   * ...
   * addressN
   */
  public byte[] serialise() {
    StringBuilder builder = new StringBuilder();
    builder.append(getVersion()).append(PayerRequest.SEPARATOR);
    if (replayDateOptional.isPresent()) {
      builder.append(replayDateOptional.get().getTime()).append(PayerRequest.SEPARATOR);
    } else {
      builder.append(OPTIONAL_NOT_PRESENT_TEXT).append(PayerRequest.SEPARATOR);
    }
    if (addressList != null) {
      for (String address : addressList) {
        builder.append(address).append(PayerRequest.SEPARATOR);
      }
    }

    log.debug("Serialised matcherResponse = \n{}", builder.toString());
    return builder.toString().getBytes(Charsets.UTF_8);

  }

  /**
   * Parse a serialised MatcherResponse
   *
   * @param serialisedMatcherResponse te serialised MatcherResponse
   *
   * @return a recreated MatcherResponse
   */
  public static MatcherResponse parse(byte[] serialisedMatcherResponse)
    throws MatcherResponseException {

    String serialisedMatcherResponseAsString = new String(serialisedMatcherResponse, Charsets.UTF_8);

    log.debug("Attempting to parse matcher response:\n{}", serialisedMatcherResponseAsString);
    String[] rows = Strings.split(serialisedMatcherResponseAsString, '\n');

    if (rows.length > 0) {
      if (Long.parseLong(rows[0]) != 1) {
        throw new MatcherResponseException("The serialisedMatcherResponse had a version of '" + rows[0] + "'. This code only understands a version of '1'");
      }

      Optional<Date> replayDateOptional;
      if (OPTIONAL_NOT_PRESENT_TEXT.equals(rows[1])) {
        replayDateOptional = Optional.absent();
      } else {
        replayDateOptional = Optional.of(new Date(Long.parseLong(rows[1])));
      }
      List<String> bitcoinAddressList = Lists.newArrayList();
      if (rows.length > 2) {
        for (int i = 2; i < rows.length; i++) {
          if (rows[i] != null && rows[i].length() > 0) {
            bitcoinAddressList.add(rows[i]);
          }
        }
      }
      return new MatcherResponse(replayDateOptional, bitcoinAddressList);

    } else {
      throw new MatcherResponseException("Cannot parse the response. Require 2 or more rows.");
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    MatcherResponse that = (MatcherResponse) o;

    if (addressList != null ? !addressList.equals(that.addressList) : that.addressList != null) return false;
    if (replayDateOptional != null ? !replayDateOptional.equals(that.replayDateOptional) : that.replayDateOptional != null)
      return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = replayDateOptional != null ? replayDateOptional.hashCode() : 0;
    result = 31 * result + (addressList != null ? addressList.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "MatcherResponse{" +
            "replayDateOptional=" + replayDateOptional +
            ", addressList=" + addressList +
            '}';
  }
}