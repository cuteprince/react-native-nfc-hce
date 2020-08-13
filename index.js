import { NativeModules, DeviceEventEmitter } from "react-native";

const { RNHce } = NativeModules;

export default {
  supportNFC: function () {
    return RNHce.supportNFC;
  },
  listenNFCStatus: function (callback) {
    DeviceEventEmitter.addListener("listenNFCStatus", resp => {
      callback(resp.status);
    });
  },
  setCardContent: function (content) {
    RNHce.setCardContent(content);
  },
  registerAids: async function (aids) {
    return await RNHce.registerAids(aids);
  },
  removeAids: async function () {
    return await RNHce.removeAids();
  }
};
