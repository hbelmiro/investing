package com.hbelmiro.investing.googlesheets;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

public interface GoogleSheetsClient {
    List<List<Object>> read(String page, String range) throws GeneralSecurityException, IOException;
}
