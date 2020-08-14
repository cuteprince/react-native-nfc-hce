declare const _default: {
  supportNFC: () => { support: boolean; enabled: boolean };
  listenNFCStatus: (callback: (enabled: boolean) => void) => void;
  setCardContent: (content: string) => void;
  registerAids: (aids: string[]) => Promise<boolean>;
  removeAids: () => Promise<boolean>;
  setSuccessToast: (content: string) => void;
  setErrorToast: (content: string) => void;
};
export default _default;
