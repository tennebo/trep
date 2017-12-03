/*
 * This code is based on the QuickStart examples from the Reuters Developer Community.
 */
package com.friggsoft.rfa.provider

import com.reuters.rfa.common.Handle
import com.reuters.rfa.dictionary.FieldDictionary
import com.reuters.rfa.omm.OMMAttribInfo
import com.reuters.rfa.omm.OMMElementEntry
import com.reuters.rfa.omm.OMMElementList
import com.reuters.rfa.omm.OMMEncoder
import com.reuters.rfa.omm.OMMFieldList
import com.reuters.rfa.omm.OMMFilterEntry
import com.reuters.rfa.omm.OMMMap
import com.reuters.rfa.omm.OMMMapEntry
import com.reuters.rfa.omm.OMMMsg
import com.reuters.rfa.omm.OMMNumeric
import com.reuters.rfa.omm.OMMPool
import com.reuters.rfa.omm.OMMQos
import com.reuters.rfa.omm.OMMState
import com.reuters.rfa.omm.OMMTypes
import com.reuters.rfa.rdm.RDMDictionary
import com.reuters.rfa.rdm.RDMInstrument
import com.reuters.rfa.rdm.RDMMsgTypes
import com.reuters.rfa.rdm.RDMService
import com.reuters.rfa.rdm.RDMUser
import com.reuters.rfa.session.omm.OMMSolicitedItemEvent

/**
 * Handle encoding of OMM messages.
 *
 * To encode an OMMMsg follow the steps below:
 *
 * 1. initialize encoder to encode MSG and provide estimated size of this message
 *    encoder.initialize(OMMTypes.MSG, estimatedMsgSize)
 *    This will allocate the encoder's buffer
 *
 * 2. get OMMMsg from the pool
 *    OMMMsg outmsg = ommPool.acquireMsg()
 *    This will allocate memory for the message to encode
 *
 * 3. set the header of the message
 *
 * 4. link the message with the encoder
 *    encoder.encodeMsgInit(outmsg, OMMTypes.NO_DATA, OMMTypes.FIELD_LIST)
 *    This will fill the encoder's buffer with the header's data and will
 *    set the encoder's to encode attribInfo and payload as the specified types.
 *    In this example the attribInfo are not being encoded (the message either
 *    does not have attribInfo or they were included in header) and the payload
 *    will be encoded as FIELD_LIST type
 *
 * 5. encode data
 *
 * 6. release the OMMMsg to memory pool
 *    ommPool.releaseMsg(outmsg)
 *    After copying data from the header to the encoder's buffer this message
 *    is not needed. The memory allocated for the message should be returned
 *    to the pool. Note: this step can be done after step 4.
 *
 * 7. returned the encoder's buffer to the calling method
 *    return (OMMMsg)encoder.getEncodedObject()
 *
 * The encoded message is in the encoder's buffer at this time.
 */
final class OmmMessageEncoder implements Closeable {

    private final OMMPool ommPool
    private final OMMEncoder encoder
    private final String serviceName
    private final FieldDictionary fieldDictionary

    OmmMessageEncoder(String serviceName, FieldDictionary fieldDictionary, OMMPool ommPool) {
        this.serviceName = serviceName
        this.fieldDictionary = fieldDictionary
        this.ommPool = ommPool
        this.encoder = ommPool.acquireEncoder()
    }

    @Override
    void close() {
        ommPool.releaseEncoder(encoder)
    }

    static void setResponseType(OMMSolicitedItemEvent event, OMMMsg outMsg) {
        assert event != null, 'event cannot be null'
        assert outMsg != null, 'outMsg cannot be null'

        if (event.getMsg().isSet(OMMMsg.Indication.REFRESH)) {
            outMsg.setRespTypeNum(OMMMsg.RespType.SOLICITED)
        } else {
            outMsg.setRespTypeNum(OMMMsg.RespType.UNSOLICITED)
        }
    }

    static void setStreamState(OMMSolicitedItemEvent event, OMMMsg outMsg) {
        assert event != null, 'event cannot be null'
        assert outMsg != null, 'outMsg cannot be null'

        if (event.getMsg().isSet(OMMMsg.Indication.NONSTREAMING)) {
            outMsg.setState(OMMState.Stream.NONSTREAMING, OMMState.Data.OK, OMMState.Code.NONE, "")
        } else {
            outMsg.setState(OMMState.Stream.OPEN, OMMState.Data.OK, OMMState.Code.NONE, "")
        }
    }

    /** Set version info from the handle. */
    static void setVersionInfo(OMMSolicitedItemEvent event, OMMMsg outMsg) {
        assert event != null, 'event cannot be null'
        assert outMsg != null, 'outMsg cannot be null'

        Handle handle = event.handle
        if (handle != null) {
            outMsg.setAssociatedMetaInfo(handle)
        }
    }

    OMMMsg encodeUpdateMsg(TickData tickData) {
        assert tickData != null, 'tickData cannot be null'

        // Set the encoder to encode an OMM message
        // The size is an estimate, but MUST be large enough for the entire message.
        // If the size is not large enough, RFA will throw.
        encoder.initialize(OMMTypes.MSG, 500)

        OMMMsg outMsg = ommPool.acquireMsg()

        // Set version info from the handle
        outMsg.setAssociatedMetaInfo(tickData.getHandle())

        // Set the message type to be an update response
        outMsg.setMsgType(OMMMsg.MsgType.UPDATE_RESP)

        // Set the message model to be MARKET_PRICE
        outMsg.setMsgModelType(RDMMsgTypes.MARKET_PRICE)

        // Set the indication flag for data not to be conflated
        outMsg.setIndicationFlags(OMMMsg.Indication.DO_NOT_CONFLATE)

        // Set the response type to be a quote update
        outMsg.setRespTypeNum(RDMInstrument.Update.QUOTE)

        // Did the original request for this item asked for attrib info to be included in the updates?
        if (tickData.getAttribInUpdates()) {
            outMsg.setAttribInfo(serviceName, tickData.getName(), RDMInstrument.NameType.RIC)
        }

        // Initialize message encoding with the update response message.
        // No data is contained in the OMMAttribInfo, so use NO_DATA for the data type of OMMAttribInfo
        // There will be data encoded in the update response message (and we know it is FieldList), so
        // use FIELD_List for the data type of the update response message.
        encoder.encodeMsgInit(outMsg, OMMTypes.NO_DATA, OMMTypes.FIELD_LIST)

        // Initialize the field list encoding.
        // Specifies that this field list has only standard data (data that is not defined in a DataDefinition)
        // DictionaryId is set to 0: The data encoded in this message used a dictionary identified by id 0.
        // Field list number is set to 1. No idea what that is.
        // Data definition id is ignored, since standard data does not use data definition.
        encoder.encodeFieldListInit(OMMFieldList.HAS_STANDARD_DATA, (short) 0, (short) 1, (short) 0)

        // TRDPRC_1:
        // Initialize the entry with the field id and data type from RDMFieldDictionary for TRDPRC_1
        encoder.encodeFieldEntryInit((short) 6, OMMTypes.REAL)
        double value = tickData.getTradePrice1()
        long longValue = NumberEncoding.roundDouble2Long(value, OMMNumeric.EXPONENT_NEG4)
        encoder.encodeReal(longValue, OMMNumeric.EXPONENT_NEG4)

        // BID:
        // Initialize the entry with the field id and data type from RDMFieldDictionary for BID
        encoder.encodeFieldEntryInit((short) 22, OMMTypes.REAL)
        value = tickData.getBid()
        longValue = NumberEncoding.roundDouble2Long(value, OMMNumeric.EXPONENT_NEG4)
        encoder.encodeReal(longValue, OMMNumeric.EXPONENT_NEG4)

        // ASK:
        // Initialize the entry with the field id and data type from RDMFieldDictionary for ASK
        encoder.encodeFieldEntryInit((short) 25, OMMTypes.REAL)
        value = tickData.getAsk()
        longValue = NumberEncoding.roundDouble2Long(value, OMMNumeric.EXPONENT_NEG4)
        encoder.encodeReal(longValue, OMMNumeric.EXPONENT_NEG4)

        // ACVOL_1:
        // Initialize the entry with the field id and data type from RDMFieldDictionary for ACVOL_1
        encoder.encodeFieldEntryInit((short) 32, OMMTypes.REAL)
        encoder.encodeReal(tickData.getAcVol1(), OMMNumeric.EXPONENT_0)

        encoder.encodeAggregateComplete()
        ommPool.releaseMsg(outMsg)

        return (OMMMsg) encoder.getEncodedObject()
    }

    OMMMsg encodeLoginRespMsg(OMMElementList elementList, boolean refreshRequested) {
        assert elementList != null, 'elementList cannot be null'

        // Set the encoder to encode an OMM message
        encoder.initialize(OMMTypes.MSG, 1000)

        OMMMsg outMsg = ommPool.acquireMsg()

        outMsg.setMsgType(OMMMsg.MsgType.REFRESH_RESP)
        outMsg.setMsgModelType(RDMMsgTypes.LOGIN)
        outMsg.setIndicationFlags(OMMMsg.Indication.REFRESH_COMPLETE)
        outMsg.setState(OMMState.Stream.OPEN, OMMState.Data.OK, OMMState.Code.NONE, "login accepted")
        outMsg.setRespTypeNum(refreshRequested? OMMMsg.RespType.SOLICITED : OMMMsg.RespType.UNSOLICITED)

        // Initialize message encoding with the login response message.
        // The attribInfo encoded in the login response message is an ElementList,
        // so use ELEMENT_LIST for the data type of attribInfo.
        // No data is contained in the payload, so use NO_DATA for the data type of payload.
        encoder.encodeMsgInit(outMsg, OMMTypes.ELEMENT_LIST, OMMTypes.NO_DATA)

        // encode attribInfo based on the attribInfo received in login request
        // and this provider supported features
        encoder.encodeElementListInit(OMMElementList.HAS_STANDARD_DATA, (short) 0, (short) 0)
        for (Iterator<?> iter = elementList.iterator(); iter.hasNext();) {
            OMMElementEntry element = (OMMElementEntry) (iter.next())

            // Set all attributes to the values received, except for DownloadConnectionConfig;
            // this attribute should not be included in login response
            if (!element.getName().equals(RDMUser.Attrib.DownloadConnectionConfig)) {
                encoder.encodeElementEntryInit(element.getName(), element.getDataType())
                encoder.encodeData(element.getData())
            }
        }
        // Set SupportStandby attribute to not supported
        encoder.encodeElementEntryInit(RDMUser.Attrib.SupportStandby, OMMTypes.UINT)
        encoder.encodeUInt((long) 0)

        // Set SupportPauseResume attribute to not supported
        encoder.encodeElementEntryInit(RDMUser.Attrib.SupportPauseResume, OMMTypes.UINT)
        encoder.encodeUInt((long) 0)

        encoder.encodeAggregateComplete()
        ommPool.releaseMsg(outMsg)

        return (OMMMsg) encoder.getEncodedObject()
    }

    OMMMsg encodeDirectoryRespMsg(OMMSolicitedItemEvent event) {
        assert event != null, 'event cannot be null'

        // set the encoder to encode an OMM message
        encoder.initialize(OMMTypes.MSG, 1000)

        // allocate memory from memory pool for the message to encode
        OMMMsg respMsg = ommPool.acquireMsg()
        setVersionInfo(event, respMsg)

        // set the data in this example by assigning arbitrary values
        respMsg.setMsgType(OMMMsg.MsgType.REFRESH_RESP)
        respMsg.setMsgModelType(RDMMsgTypes.DIRECTORY)
        respMsg.setIndicationFlags(OMMMsg.Indication.REFRESH_COMPLETE)

        setStreamState(event, respMsg)
        setResponseType(event, respMsg)

        respMsg.setItemGroup(1)

        OMMAttribInfo outAttribInfo = ommPool.acquireAttribInfo()

        // AttribInfo value specifies what type of information is provided in directory response.
        // Encode the information that is being requested.
        // This application supports only INFO, STATE, and GROUP.
        if (event.getMsg().has(OMMMsg.HAS_ATTRIB_INFO)) {
            OMMAttribInfo at = event.getMsg().getAttribInfo()
            if (at.has(OMMAttribInfo.HAS_FILTER)) {
                // Set the filter information to what was requested.
                outAttribInfo.setFilter(at.getFilter())
            }
        }

        // Set the attribInfo into the message.
        respMsg.setAttribInfo(outAttribInfo)

        // Initialize the response message encoding that encodes no data for attribInfo,
        // and encodes payload with the MAP data type.
        encoder.encodeMsgInit(respMsg, OMMTypes.NO_DATA, OMMTypes.MAP)

        // Map encoding initialization.
        // Specifies the flag for the map, the data type of the key as ascii_string, as defined by RDMUsageGuide.
        // the data type of the map entries is FilterList, the total count hint, and the dictionary id as 0
        encoder.encodeMapInit(OMMMap.HAS_TOTAL_COUNT_HINT, OMMTypes.ASCII_STRING, OMMTypes.FILTER_LIST, 1, (short) 0)

        // MapEntry: Each service is associated with a map entry with the service name has the key.
        encoder.encodeMapEntryInit(0, OMMMapEntry.Action.ADD, null)
        encoder.encodeString(serviceName, OMMTypes.ASCII_STRING)

        // Filter list encoding initialization.
        // Specifies the flag, the data type in the filter entry as element list, and total count hint of 2 entries.
        encoder.encodeFilterListInit(0, OMMTypes.ELEMENT_LIST, 2)

        // Provide INFO filter if application requested.
        if ((outAttribInfo.getFilter() & RDMService.Filter.INFO) != 0) {
            // Specifies the filter entry has data, action is SET, the filter id is the filter information, and data type is elementlist.  No permission data is provided.
            encoder.encodeFilterEntryInit(OMMFilterEntry.HAS_DATA_FORMAT, OMMFilterEntry.Action.SET, RDMService.FilterId.INFO, OMMTypes.ELEMENT_LIST, null)

            encoder.encodeElementListInit(OMMElementList.HAS_STANDARD_DATA, (short) 0, (short) 0)
            encoder.encodeElementEntryInit(RDMService.Info.Name, OMMTypes.ASCII_STRING)
            encoder.encodeString(serviceName, OMMTypes.ASCII_STRING)
            encoder.encodeElementEntryInit(RDMService.Info.Vendor, OMMTypes.ASCII_STRING)
            encoder.encodeString("Reuters", OMMTypes.ASCII_STRING)
            encoder.encodeElementEntryInit(RDMService.Info.IsSource, OMMTypes.UINT)
            encoder.encodeUInt(0L)
            encoder.encodeElementEntryInit(RDMService.Info.Capabilities, OMMTypes.ARRAY)
            // Pass 0 as the size. This lets the encoder calculate the size for each UINT.
            encoder.encodeArrayInit(OMMTypes.UINT, 0)

            encoder.encodeArrayEntryInit()
            encoder.encodeUInt((long) RDMMsgTypes.DICTIONARY)
            encoder.encodeArrayEntryInit()
            encoder.encodeUInt((long) RDMMsgTypes.MARKET_PRICE)
            encoder.encodeAggregateComplete()    // Completes the Array.
            encoder.encodeElementEntryInit(RDMService.Info.DictionariesProvided, OMMTypes.ARRAY)
            encoder.encodeArrayInit(OMMTypes.ASCII_STRING, 0)
            encoder.encodeArrayEntryInit()
            encoder.encodeString("RWFFld", OMMTypes.ASCII_STRING)
            encoder.encodeArrayEntryInit()
            encoder.encodeString("RWFEnum", OMMTypes.ASCII_STRING)
            encoder.encodeAggregateComplete() // Completes the Array.
            encoder.encodeElementEntryInit(RDMService.Info.DictionariesUsed, OMMTypes.ARRAY)
            encoder.encodeArrayInit(OMMTypes.ASCII_STRING, 0)
            encoder.encodeArrayEntryInit()
            encoder.encodeString("RWFFld", OMMTypes.ASCII_STRING)
            encoder.encodeArrayEntryInit()
            encoder.encodeString("RWFEnum", OMMTypes.ASCII_STRING)
            encoder.encodeAggregateComplete()  // Completes the Array.
            encoder.encodeElementEntryInit(RDMService.Info.QoS, OMMTypes.ARRAY)
            encoder.encodeArrayInit(OMMTypes.QOS, 0)
            encoder.encodeArrayEntryInit()
            encoder.encodeQos(OMMQos.QOS_REALTIME_TICK_BY_TICK)
            encoder.encodeAggregateComplete()     // Completes the Array.
            encoder.encodeAggregateComplete()     // Completes the ElementList
        }

        // Provide STATE filter if requested.
        if ((outAttribInfo.getFilter() & RDMService.Filter.STATE) != 0) {
            encoder.encodeFilterEntryInit(OMMFilterEntry.HAS_DATA_FORMAT, OMMFilterEntry.Action.UPDATE, RDMService.FilterId.STATE, OMMTypes.ELEMENT_LIST, null)
            encoder.encodeElementListInit(OMMElementList.HAS_STANDARD_DATA, (short) 0, (short) 0)
            encoder.encodeElementEntryInit(RDMService.SvcState.ServiceState, OMMTypes.UINT)
            encoder.encodeUInt(1L)
            encoder.encodeElementEntryInit(RDMService.SvcState.AcceptingRequests, OMMTypes.UINT)
            encoder.encodeUInt(1L)
            encoder.encodeElementEntryInit(RDMService.SvcState.Status, OMMTypes.STATE)
            encoder.encodeState(OMMState.Stream.OPEN, OMMState.Data.OK, OMMState.Code.NONE, "")
            encoder.encodeAggregateComplete()  // Completes the ElementList.
        }

        encoder.encodeAggregateComplete() // This one is for FilterList
        encoder.encodeAggregateComplete() // This one is for Map

        ommPool.releaseAttribInfo(outAttribInfo)
        ommPool.releaseMsg(respMsg)

        return ((OMMMsg) encoder.getEncodedObject())
    }

    /** Encoding of the enum dictionary. */
    OMMMsg encodeEnumDictionary(OMMSolicitedItemEvent event) {
        assert event != null, 'event cannot be null'

        // set the encoder to encode an OMM message
        encoder.initialize(OMMTypes.MSG, 250000)

        // allocate memory from memory pool for the message to encode
        OMMMsg outMsg = ommPool.acquireMsg()

        setVersionInfo(event, outMsg)

        outMsg.setMsgType(OMMMsg.MsgType.REFRESH_RESP)
        outMsg.setMsgModelType(RDMMsgTypes.DICTIONARY)
        outMsg.setIndicationFlags(OMMMsg.Indication.REFRESH_COMPLETE)

        setResponseType(event, outMsg)
        setStreamState(event, outMsg)

        outMsg.setItemGroup(1)
        OMMAttribInfo attribInfo = ommPool.acquireAttribInfo()
        attribInfo.setServiceName(serviceName)
        attribInfo.setName("RWFEnum")
        attribInfo.setFilter(RDMDictionary.Filter.NORMAL)
        outMsg.setAttribInfo(attribInfo)

        encoder.encodeMsgInit(outMsg, OMMTypes.NO_DATA, OMMTypes.SERIES)
        FieldDictionary.encodeRDMEnumDictionary(fieldDictionary, encoder)

        ommPool.releaseAttribInfo(attribInfo)
        ommPool.releaseMsg(outMsg)

        return (OMMMsg) encoder.getEncodedObject()
    }

    /** Encoding of the RDMFieldDictionary. */
    OMMMsg encodeFldDictionary(OMMSolicitedItemEvent event) {
        assert event != null, 'event cannot be null'

        // Allocate memory from memory pool for the message to encode
        // number_of_fids * 60 (approximate size per row)
        int encoderSizeForFieldDictionary = fieldDictionary.size() * 60
        encoder.initialize(OMMTypes.MSG, encoderSizeForFieldDictionary)

        OMMMsg outMsg = ommPool.acquireMsg()

        setVersionInfo(event, outMsg)

        outMsg.setMsgType(OMMMsg.MsgType.REFRESH_RESP)
        outMsg.setMsgModelType(RDMMsgTypes.DICTIONARY)
        outMsg.setIndicationFlags(OMMMsg.Indication.REFRESH_COMPLETE)

        setResponseType(event, outMsg)
        setStreamState(event, outMsg)

        outMsg.setItemGroup(1)
        OMMAttribInfo attribInfo = ommPool.acquireAttribInfo()
        attribInfo.setServiceName(serviceName)
        attribInfo.setName("RWFFld")
        attribInfo.setFilter(RDMDictionary.Filter.NORMAL)
        outMsg.setAttribInfo(attribInfo)

        encoder.encodeMsgInit(outMsg, OMMTypes.NO_DATA, OMMTypes.SERIES)
        FieldDictionary.encodeRDMFieldDictionary(fieldDictionary, encoder)

        ommPool.releaseAttribInfo(attribInfo)
        ommPool.releaseMsg(outMsg)

        return (OMMMsg) encoder.getEncodedObject()
    }

    OMMMsg encodeRefreshMsg(OMMSolicitedItemEvent event, TickData tickData) {
        assert event != null, 'event cannot be null'
        assert tickData != null, 'tickData cannot be null'

        // Set the encoder to encode an OMM message
        encoder.initialize(OMMTypes.MSG, 500)

        OMMMsg outMsg = ommPool.acquireMsg()

        setVersionInfo(event, outMsg)

        // Set the message type to be refresh response.
        outMsg.setMsgType(OMMMsg.MsgType.REFRESH_RESP)
        // Set the message model type to be the type requested.
        outMsg.setMsgModelType(event.getMsg().getMsgModelType())
        // Indicates this message will be the full refresh; or this is the last refresh in the multi-part refresh.
        outMsg.setIndicationFlags(OMMMsg.Indication.REFRESH_COMPLETE)
        // Set the item group to be 2.  Indicates the item will be in group 2.
        outMsg.setItemGroup(2)

        setResponseType(event, outMsg)
        setStreamState(event, outMsg)

        // This code encodes refresh for MARKET_PRICE domain
        outMsg.setIndicationFlags(OMMMsg.Indication.REFRESH_COMPLETE)

        outMsg.setAttribInfo(event.getMsg().getAttribInfo())
        encoder.initialize(OMMTypes.MSG, 1000)
        encoder.encodeMsgInit(outMsg, OMMTypes.NO_DATA, OMMTypes.FIELD_LIST)

        encoder.encodeFieldListInit(
                OMMFieldList.HAS_STANDARD_DATA | OMMFieldList.HAS_INFO,
                (short) 0, (short) 1, (short) 0)

        // RDNDISPLAY
        encoder.encodeFieldEntryInit((short) 2, OMMTypes.UINT)
        encoder.encodeUInt(100L)

        // RDN_EXCHID
        encoder.encodeFieldEntryInit((short) 4, OMMTypes.ENUM)
        encoder.encodeEnum(155)

        // DIVIDEND_DATE
        encoder.encodeFieldEntryInit((short) 38, OMMTypes.DATE)
        encoder.encodeDate(2006, 12, 25)

        // TRDPRC_1
        encoder.encodeFieldEntryInit((short) 6, OMMTypes.REAL)
        double value = tickData.getTradePrice1()
        long longValue = NumberEncoding.roundDouble2Long(value, OMMNumeric.EXPONENT_NEG4)
        encoder.encodeReal(longValue, OMMNumeric.EXPONENT_NEG4)

        // BID
        encoder.encodeFieldEntryInit((short) 22, OMMTypes.REAL)
        value = tickData.getBid()
        longValue = NumberEncoding.roundDouble2Long(value, OMMNumeric.EXPONENT_NEG4)
        encoder.encodeReal(longValue, OMMNumeric.EXPONENT_NEG4)

        // ASK
        encoder.encodeFieldEntryInit((short) 25, OMMTypes.REAL)
        value = tickData.getAsk()
        longValue = NumberEncoding.roundDouble2Long(value, OMMNumeric.EXPONENT_NEG4)
        encoder.encodeReal(longValue, OMMNumeric.EXPONENT_NEG4)

        // ACVOL_1
        encoder.encodeFieldEntryInit((short) 32, OMMTypes.REAL)
        encoder.encodeReal(tickData.getAcVol1(), OMMNumeric.EXPONENT_0)

        // ASK_TIME
        encoder.encodeFieldEntryInit((short) 267, OMMTypes.TIME)
        encoder.encodeTime(19, 12, 23, 0)

        encoder.encodeAggregateComplete()
        ommPool.releaseMsg(outMsg)

        return (OMMMsg) encoder.getEncodedObject()
    }
}
