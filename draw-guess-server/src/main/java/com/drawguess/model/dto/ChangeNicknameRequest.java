package com.drawguess.model.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangeNicknameRequest {

    @Size(min = 1, max = 20, message = "昵称长度1-20个字符")
    private String nickname;
}
