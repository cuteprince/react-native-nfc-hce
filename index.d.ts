declare const _default: {
  supportNFC: () => { support: boolean; enabled: boolean };
  listenNFCStatus: (callback: (enabled: boolean) => void) => void;
  setCardContent: (content: string) => void;
  registerAids: (aids: string[]) => Promise<boolean>;
  removeAids: () => Promise<boolean>;
};
export default _default;
