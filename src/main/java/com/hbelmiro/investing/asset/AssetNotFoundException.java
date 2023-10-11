package com.hbelmiro.investing.asset;

public class AssetNotFoundException extends RuntimeException {

    public AssetNotFoundException(Asset asset) {
        super("Could not find an asset: " + asset);
    }
}
