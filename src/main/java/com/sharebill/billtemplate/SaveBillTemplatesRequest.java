package com.sharebill.billtemplate;

import java.util.List;

public record SaveBillTemplatesRequest(
    List<String> labels
) {
}
