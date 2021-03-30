package matchdog;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;

public class LastMatchPosition implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    String matchId;
    String positionId;

    LastMatchPosition(String mid, String posId) {
        matchId = mid;
        positionId = posId;
    }
}
