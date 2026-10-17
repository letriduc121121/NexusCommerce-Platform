export interface ReviewImage {
  id: string;
  imageUrl: string;
}

export interface ReviewResponse {
  id: string;
  orderItemId: string;
  productId: string;
  userId: string;
  userFullName: string;
  rating: number;
  comment?: string;
  variantInfo?: string;
  coinsAwarded: number;
  rewardCoins?: number;
  isHidden: boolean;
  isEdited: boolean;
  shopReply?: string;
  shopRepliedAt?: string;
  images: string[];
  createdAt: string;
}

export interface CreateReviewRequest {
  orderItemId: string;
  rating: number;
  comment?: string;
  variantInfo?: string;
  photoUrls?: string[];
}

export interface UpdateReviewRequest {
  rating?: number;
  comment?: string;
  photoUrls?: string[];
}

export interface VerifyPurchaseResponse {
  eligible: boolean;
  canReview?: boolean;
  alreadyReviewed?: boolean;
  orderItemId?: string;
  orderId?: string;
  productId?: string;
  userId?: string;
  variantInfo?: string;
  message?: string;
}
