CREATE TABLE messages (
  `id` INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  `user_id` INT,
  `project_id` INT,
  `message_type` VARCHAR(128),
  `content` VARCHAR(128),
  `create_time` timestamp default current_timestamp
);

create unique index ix_message_id on messages (`id`);